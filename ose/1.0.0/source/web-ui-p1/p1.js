(() => {
'use strict';
const A='../assets/ose-user-pack-v1/source/TALES_VTT_ASSET_PACK/assets_individuais/';
const q=new URLSearchParams(window.__P1_CAPTURE_QUERY||location.search);
let screen=q.get('screen')||'session';
let stateName=q.get('state')||'active';
let viewState=null;
let companionOpinions=null;
let geminiState={textSceneConfigured:false,ttsAudioConfigured:false,textModel:'',imageModel:''};
let ttsState={state:'PREPARING',ready:false,enginePackage:'',voiceName:'',speechRate:1,engines:[],voices:[],mode:'ANDROID_INSTALLED_ENGINE'};
let mediaTarget=null;
let mapSelection=null;
let mapMode='AUTO';
let diceHistory=[];
let backupBuffer='';
let ruleLibrary=[];
let ruleLibraryLoaded=false;
let libraryQuery='';
let lastRuleTrace=null;
let localLog=['Campanha carregada.'];
let editingCharacter=false;
let creatingAdditionalCharacter=false;
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
  if(m.type==='ViewState'){viewState=p;if(window.vttLifeAdapter)window.vttLifeAdapter.initFromViewState(viewState);geminiState=p.gemini||geminiState;ttsState=p.tts?{...ttsState,...p.tts}:ttsState;lastRuleTrace=p.lastRuleTrace||lastRuleTrace;render();checkFirstLaunch();}
  else if(m.type==='SessionUpdate'){
   const previous=viewState;
   if(p.viewState){viewState=p.viewState;geminiState=viewState.gemini||geminiState;}
   if(window.vttLifeAdapter)window.vttLifeAdapter.initFromViewState(viewState);
   lastRuleTrace=p.ruleTrace||viewState?.lastRuleTrace||lastRuleTrace;
   if(Array.isArray(p.diceRolls) && p.diceRolls.length > 0){
     const rolls = p.diceRolls.map(d => d.value);
     const total = rolls.reduce((a, b) => a + b, 0);
     const terms = [];
     p.diceRolls.forEach(d => {
       const sides = d.sides || 6;
       const last = terms[terms.length - 1];
       if (last && last.sides === sides) { last.count++; } else { terms.push({ count: 1, sides: sides, sign: 1 }); }
     });
     const formula = terms.map(t => `${t.count}d${t.sides}`).join(' + ');
     const primarySides = p.diceRolls[0].sides || 6;
     el('dice3dOverlay')?.classList.remove('hidden');
     window.BraseiroDice?.start({
       sides: primarySides,
       count: p.diceRolls.length,
       terms: terms,
       modifier: 0,
       canonicalTotal: total,
       rolls: rolls,
       targetRolls: rolls,
       formula: formula
     });
   }
   localLog.push(p.mechanicalMutation?'A ação foi resolvida e o mundo foi atualizado.':'O Mestre respondeu sem alterar o estado mecânico.');
   render();showToast('Ação concluída.');announceStateChanges(previous,viewState,p.feedback||'');
  }
  else if(m.type==='CharacterCreationDraft'||m.type==='CharacterCreationResult'){if(p.viewState)viewState=p.viewState;if(m.type==='CharacterCreationResult'&&p.ok)creatingAdditionalCharacter=false;render();showToast(p.ok===false?'Não foi possível concluir a criação.':'Criação atualizada.');}
  else if(m.type==='CharacterCreationCancelResult'){if(p.viewState)viewState=p.viewState;creatingAdditionalCharacter=false;editingCharacter=false;render();showToast('Criação cancelada.');}
  else if(m.type==='PlayerGuideModeResult'){if(p.viewState)viewState=p.viewState;render();showToast('Modo de orientação alterado.');}
  else if(m.type==='CharacterSelectionResult'){if(p.viewState)viewState=p.viewState;editingCharacter=false;creatingAdditionalCharacter=false;screen='sheet';render();}
  else if(m.type==='CompanionOpinionsResult'){if(p.readOnly===true&&p.mechanicalDelta===false){companionOpinions=p;render();showToast('Opiniões consultadas sem avançar a cena.')}else showToast('Resposta de opinião inválida.');}
  else if(m.type==='CharacterControlResult'||m.type==='MasterActionResult'){if(p.viewState)viewState=p.viewState;render();showToast(p.ok===false?'A decisão do Mestre não foi concluída. O último turno do jogador continua salvo.':m.type==='CharacterControlResult'?'Controle do personagem atualizado.':'Decisão do Mestre atualizada.');}
  else if(m.type==='CharacterDeleteResult'){if(p.viewState)viewState=p.viewState;editingCharacter=false;creatingAdditionalCharacter=false;render();showToast('Ficha excluída da companhia; histórico preservado.');}
  else if(m.type==='UiCommandResult'){if(p.viewState)viewState=p.viewState;render();showToast(uiStatusMessage(p));}
  else if(m.type==='GMHelpResponse')showToast(p.answer||'Sem resposta do Mestre.');
  else if(m.type==='TtsState'){ttsState={...ttsState,...p};updateTtsSettings();showToast(p.state==='ERROR'?`TTS: ${p.message||'falha ao reproduzir o áudio.'}`:ttsMessage(p.state));if(p.state==='IDLE')postNative('TtsSettingsGet',{});}
  else if(m.type==='TtsSettingsState'){ttsState={...ttsState,...p};updateTtsSettings();}
  else if(m.type==='GeminiSettingsState'){geminiState=p;updateSettingsStatus();}
  else if(m.type==='GeminiCredentialTestResult')showToast(p.ok?'Conexão Gemini: OK':'Não foi possível usar a configuração informada.');
  else if(m.type==='SceneImageResult'){postNative('ViewState',{request:'after-scene-image'});showToast('Ilustração da cena atualizada.');}
  else if(m.type==='ScenePromptShareResult')showToast('Abrindo o Gemini com o prompt da cena…');
  else if(m.type==='PortraitPromptShareResult')showToast('Abrindo o Gemini com o prompt do retrato…');
  else if(m.type==='PresentationImageSaved'){if(p.viewState)viewState=p.viewState;else postNative('ViewState',{request:'after-media-save'});render();showToast(p.scope==='SCENE'?'Imagem da cena atualizada.':p.scope==='CHARACTER'?'Retrato do personagem atualizado.':p.scope==='NPC'?'Retrato do NPC atualizado.':'Imagem salva e vinculada.');}
  else if(m.type==='PresentationImageRemoved'){if(p.viewState)viewState=p.viewState;else postNative('ViewState',{request:'after-media-remove'});render();showToast('Imagem removida com sucesso.');}
  else if(m.type==='AdventureImportResult'){if(p.viewState)viewState=p.viewState;render();showToast(p.ok?`Aventura carregada: ${p.title||''}`:p.cancelled?'Importação cancelada.':'Erro ao importar aventura.');}
  else if(m.type==='AdventureFinishResult'){if(p.viewState)viewState=p.viewState;render();showToast(p.ok?'Aventura concluída. O legado e o mundo foram preservados.':'Nenhuma aventura ativa.');}
  else if(m.type==='ManualDiceResult'){
    const formula = `${p.count}d${p.sides}${p.modifier ? (p.modifier > 0 ? '+' : '') + p.modifier : ''}`;
    diceHistory.unshift({rolls:p.rolls||[],total:p.total,sides:p.sides,count:p.count,modifier:p.modifier||0,formula});
    diceHistory=diceHistory.slice(0,12);
    render();
    showToast(`Resultado: ${p.total}`);
    el('dice3dOverlay')?.classList.remove('hidden');
    window.BraseiroDice?.start({
      sides: p.sides,
      count: p.count,
      modifier: p.modifier || 0,
      canonicalTotal: p.total,
      rolls: p.rolls || [],
      formula: formula
    });
  }
  else if(m.type==='RuleEvidenceListResult'){ruleLibrary=p.rules||[];ruleLibraryLoaded=true;render();}
  else if(m.type==='RuleEvidenceResult'){showRuleDetail(p);}
  else if(m.type==='ResetAdventureResult'){if(p.viewState)viewState=p.viewState;lastRuleTrace=null;ruleLibraryLoaded=false;ruleLibrary=[];screen='session';render();showToast('A aventura foi recomeçada.');}
  else if(m.type==='CharacterSheetExportResult'){if(p.ok)showToast(`Ficha salva em ${p.fileName||'arquivo JSON'}.`);}
  else if(m.type==='CharacterSheetImportResult'){if(p.viewState)viewState=p.viewState;render();if(p.ok)showToast('Ficha importada.');}
  else if(m.type==='CharacterSheetUpdateResult'){if(p.viewState)viewState=p.viewState;editingCharacter=false;render();showToast('Ficha atualizada.');}
  else if(m.type==='CharacterRecreateResult'){if(p.viewState)viewState=p.viewState;editingCharacter=false;screen='sheet';render();showToast('Personagem anterior removido. Crie o novo aventureiro.');}
  else if(m.type==='BackupExportResult'){backupBuffer=p.backup||'';const t=el('backupText');if(t)t.value=backupBuffer;showToast('Backup gerado. Copie e guarde o texto.');}
  else if(m.type==='BackupImportResult'){if(p.viewState)viewState=p.viewState;render();showToast('Backup restaurado.');}
  else if(m.type==='BridgeError'){console.error('[Braseiro OSE] native bridge action failed',p.operation||'unknown',p.message||'unknown');document.querySelectorAll('[data-delete-character],[data-random-character],[data-commit-draft],[data-save-character],[data-start-draft],[data-character-select],[data-character-mode],[data-curator-approve],[data-curator-change],[data-curator-reject],[data-master-continue],[data-companion-opinions]').forEach(x=>x.disabled=false);showToast(friendlyBridgeError(p.message));}
 }catch(_e){console.error('[Braseiro OSE] invalid native response',_e);showToast('Falha ao interpretar a resposta: '+String(_e&&_e.message||_e).slice(0,130));}
};

function uiStatusMessage(p){if(p.ok===false)return 'A ação não pôde ser concluída pelas regras atuais.';if(p.status==='READY_TO_START')return 'Equipamento concluído. Agora você pode começar a aventura.';return 'Equipamento atualizado.'}
function friendlyBridgeError(code){
 const errorCode=String(code||'ERRO_DESCONHECIDO').trim().slice(0,100);
 if(/^GEMINI_HTTP_(400|401|402|403|404|429)$/.test(errorCode)){
   const suffix=errorCode.split('_').pop();
   if(suffix==='429')return 'Gemini: cota excedida. Use ABRIR GEMINI no menu de imagem ou importe pela galeria.';
   if(suffix==='402'||suffix==='403')return 'Gemini: a geração via API não está liberada para sua chave/cota gratuita. Use ABRIR GEMINI no menu e compartilhe a imagem.';
   if(suffix==='401')return 'Gemini: chave não autorizada. Confira a configuração; a opção Gemini externo continua disponível.';
   if(suffix==='404')return 'Gemini: modelo de imagem indisponível. Verifique o modelo nas configurações.';
   return 'Gemini: solicitação de imagem recusada ('+errorCode+'). Verifique o modelo ou use a opção externa.';
 }
 if(errorCode==='GEMINI_IMAGE_OUTPUT_MISSING')return 'Gemini não retornou uma imagem. Tente outra descrição ou use a opção externa.';
 if(errorCode==='GEMINI_TEXT_SCENE_KEY_MISSING')return 'Não há chave de API configurada. O Gemini externo pode ser usado sem configurar a API.';
 const m={GEMINI_KEY_MISSING:'Configure a chave Gemini.',INVALID_GEMINI_MODEL:'Confira o nome do modelo Gemini.',INVALID_TTS_VOICE:'Confira o nome da voz TTS.',EMPTY_VISIBLE_NARRATION:'Ainda não há narração para ouvir.',NO_CHARACTER:'Crie ou importe um personagem primeiro.',CHARACTER_ALREADY_EXISTS:'Já existe uma ficha com este identificador.',RANDOM_CHARACTER_NO_VALID_CLASS:'Não foi possível gerar uma classe válida para estes atributos.',INVALID_CHARACTER_SHEET:'A ficha JSON não é válida para o BRASEIRO OSE.',CHARACTER_PROFILE_MISMATCH:'A ficha pertence a outro perfil OSE.',CHARACTER_ID_EXISTS:'Já existe um personagem com este identificador.',CHARACTER_ITEM_PROFILE_MISMATCH:'Um item da ficha pertence a outro perfil OSE.',ITEM_ID_EXISTS:'Já existe um item com este identificador.',INVALID_CHARACTER_NAME:'Digite um nome válido para o personagem.',CHARACTER_RECREATE_AFTER_START:'A recriação pré-início não está disponível; use EXCLUIR FICHA.',UNKNOWN_CHARACTER_ID:'Este personagem não está na companhia.',CHARACTER_SELECTION_SAVE_FAILED:'Não foi possível salvar a seleção da ficha.',INVALID_CHARACTER_COMMAND:'Comando incompleto. Tente novamente.',CHARACTER_COMMAND_ALREADY_APPLIED:'Este comando já foi realizado. Atualize a companhia.',CHARACTER_PLAYER_CONTROLLED:'Este personagem só pode agir por sua decisão.',CHARACTER_MASTER_CONTROLLED:'Este personagem é controlado pelo Mestre.',CHARACTER_CURATOR_APPROVAL_REQUIRED:'A ação precisa da sua aprovação antes de ser executada.',PROPOSAL_ALREADY_REVIEWED:'Esta proposta já foi revisada.',CHARACTER_NOT_CURATED:'Este personagem não está no modo de curadoria.',CHARACTER_ACTOR_PROTECTED:'O Mestre não pode decidir pelo personagem protegido de outro controlador.',GROUP_ACTION_REQUIRES_PLAYER_DECISION:'Essa ação afeta toda a companhia e depende da decisão humana.'};return m[errorCode]||('Falha ao executar a ação ('+errorCode+'). Abra as configurações ou tente novamente.');}
function ttsMessage(s){return s==='PREPARING'?'Preparando a narração em voz.':s==='PLAYING'?'Narração em andamento.':s==='STOPPED'?'Áudio interrompido.':s==='IDLE'?'Motor de voz pronto.':s==='ERROR'?'Falha no motor de voz.':'TTS atualizado.'}
function showToast(msg){const t=el('toast');if(!t)return;t.textContent=msg;t.classList.remove('hidden');clearTimeout(showToast.timer);showToast.timer=setTimeout(()=>t.classList.add('hidden'),/falha|erro|cota|não está|não retornou|gratuita|compartilhe/i.test(String(msg))?6200:3500)}
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
 const titles={session:'SESSÃO DE JOGO',map:'MAPA',sheet:'FICHA DO PERSONAGEM',dice:'MESA DE DADOS',library:'BIBLIOTECA OSE',world:'MUNDO VIVO',company:'COMPANHIA'};
 el('headerTitle').textContent=titles[screen]||'BRASEIRO OSE';
 const tm=viewState?.time||{};el('timeStatus').textContent=`DIA ${(tm.days??0)+1} · T${tm.turns??0}`;
 el('phaseStatus').textContent=phaseLabel();el('torchStatus').textContent=resource('torch','0');el('rationStatus').textContent=resource('rations','0');
 document.querySelectorAll('.navbtn').forEach(b=>b.classList.toggle('active',b.dataset.screen===screen));
}
function go(s){if(s!=='sheet'){creatingAdditionalCharacter=false;editingCharacter=false;}screen=s;q.set('screen',s);try{history.replaceState(null,'',`${location.pathname}?${q}`)}catch(_e){}render()}
function render(){updateHeader();main.innerHTML=screen==='session'?renderSession():screen==='map'?renderMap():screen==='sheet'?renderSheet():screen==='dice'?renderDiceScreen():screen==='library'?renderLibrary():screen==='world'?renderWorld():renderCompany();window.OSEEquipmentArt?.refresh(main);wireCurrentScreen();checkFirstLaunch();if(screen==='library'&&hasNative()&&!ruleLibraryLoaded)postNative('RuleEvidenceList',{});}

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
 const npcPanel=presentNpcs.length?`<div class="panel"><h3>NPCs presentes</h3><div class="text-entity-list">${presentNpcs.map(n=>{const nPort=n.portraitUrl||viewState?.presentation?.npcPortraits?.[n.npcId];return `<div class="text-entity">${nPort?`<img src="${esc(nPort)}" class="avatar-thumb" alt="Retrato">`:''}<b>${esc(n.name)}</b>${hasNative()?`<button class="photo-action" data-photo-npc="${esc(n.npcId)}" title="Retrato do NPC">📷</button>`:''}</div>`}).join('')}</div></div>`:'';
 const receipt = viewState?.narrationReceipt;
 const narrativeStatus = String(receipt?.status || '').toUpperCase();
 const isPartialNarration = narrativeStatus === 'PARTIAL_LENGTH';
 const narrationOrigin = receipt?.provider === 'GEMINI_TEXT'
   ? 'Mestre Gemini (' + esc(receipt.model || 'neural') + ')'
   : 'Narração local offline';
 const narrationNotice = isPartialNarration
   ? 'Cena abreviada: o Gemini não atingiu a extensão solicitada. O texto recebido foi preservado.'
   : narrativeStatus === 'FALLBACK'
     ? 'Gemini indisponível nesta resposta; o Mestre utilizou a narração local.'
     : narrativeStatus === 'LOCAL_ONLY'
       ? 'Narração local: chave Gemini de texto não configurada.'
       : narrationOrigin;
 const badgeHtml = receipt
   ? `<div class="narration-badge ${esc(narrativeStatus.toLowerCase())}" role="status">${esc(narrationNotice)}</div>`
   : '';
 return `<div class="kicker">Sessão ativa</div>
  <div class="panel">${badgeHtml}<div class="row"><div><span class="pill">${profileLabel()}</span><h2 style="margin-top:7px">Cena atual</h2></div></div>
  ${sceneUrl?img(sceneUrl,'Ilustração da cena atual','scene-image'):''}
  <div id="visibleNarration" class="narrative-text">${paragraphs(narration)}</div>
  <div class="session-tools">
  <button class="small" data-tts="play">▶ OUVIR</button>
  <button class="small" data-tts="stop">■ PARAR</button>
  <button class="small" data-tts="replay">↻ REPETIR</button>
  <button class="small" data-scene-photo data-scene-id="${esc(sceneId)}" title="Menu de imagem da cena">📷 CENA</button>
  <button class="small" data-guide-topic="rule_trace" title="Explica como o árbitro resolveu mecanicamente a última ação">❓ REGRA DA AÇÃO</button>
</div></div>
  ${npcPanel}
  ${renderAdventureCard()}
  ${renderCuratorPanel()}
  ${renderCompanionOpinions()}
  ${renderMasterContinue()}
  ${renderRuleTrace(lastRuleTrace||viewState?.lastRuleTrace)}
  <div class="panel"><h3>O que você faz?</h3><button id="openSuggestionsBtn" class="secondary" style="margin-bottom:8px;width:100%">💡 O QUE POSSO FAZER AGORA?</button><div class="suggestions">${ds.map(x=>`<button class="suggestion" data-suggest="${esc(x.reaction||x[1]||'')}" data-intent="${esc(x.mechanicalIntent||'')}">${esc(x.label||x[0]||'AÇÃO')}</button>`).join('')}</div><textarea id="reactionInput" placeholder="Ex.: examino as pegadas, pergunto o que há por perto, sigo com cuidado..."></textarea><button class="primary" data-react ${hasNative()&&!(viewState?.party||[]).some(m=>(m.controlMode||'PLAYER')==='PLAYER')?'disabled':''}>Enviar ação</button><button class="secondary" style="margin-top:7px" data-go="dice">Abrir rolagem de dados</button></div>
  <div class="panel"><h3>Diário</h3><div class="log friendly-log">${esc(localLog.slice(-8).join('\n')||'Sem eventos registrados.')}</div></div>`;
}
function renderAdventureCard(){
 const adv=viewState?.activeAdventure;
 if(!adv)return `<div class="panel adventure-panel"><h3>Aventuras</h3><p class="note">Nenhum pacote de aventura ativo. O mundo segue em sandbox ou você pode importar um pacote JSON.</p><div class="row"><button class="small primary" data-adventure-import>IMPORTAR AVENTURA JSON</button><button class="small secondary" data-adventure-load-classic>EXEMPLO CLÁSSICO</button><button class="small secondary" data-adventure-load-advanced>EXEMPLO AVANÇADO</button></div></div>`;
 return `<div class="panel adventure-panel"><div class="row"><div><span class="pill">${esc(adv.system||'OSE')}</span><h3 style="margin-top:6px">${esc(adv.title||'Aventura')}</h3></div></div><p><b>Premissa:</b> ${esc(adv.premise||'')}</p><p class="note">${adv.locationsCount||0} locais cadastrados · ${adv.npcsCount||0} NPCs registrados · ${adv.objectivesCount||0} objetivos</p><div class="row" style="margin-top:8px"><button class="small secondary" data-adventure-import>SUBSTITUIR JSON</button><button class="small secondary" data-adventure-finish>CONCLUIR AVENTURA (PRESERVAR MUNDO)</button></div></div>`;
}
function loadSampleAdventure(path){
 fetch(path).then(r=>r.text()).then(raw=>{
  postNative('AdventureImport',{rawJson:raw});
  showToast('Carregando aventura canônica…');
 }).catch(_e=>{
  showToast('Não foi possível ler o arquivo da aventura.');
 });
}
function paragraphs(text){return String(text||'').split(/\n\s*\n/).filter(Boolean).map(p=>`<p>${esc(p)}</p>`).join('')}
function traceStatusLabel(s){const m={CANONICAL_RULE:'Regra canônica',CANONICAL_PROCEDURE:'Procedimento canônico',OPTIONAL_RULE:'Regra opcional',PRODUCT_PRESET:'Contrato do produto',PROCEDURAL_TABLE:'Tabela/procedimento',ADVENTURE_LOCAL_RULE:'Regra da aventura',REFEREE_JUDGMENT:'Julgamento do Mestre',MISSING_EVIDENCE:'Evidência insuficiente'};return m[s]||String(s||'Sem rastreio')}
function renderRuleTrace(t){if(!t)return `<div class="panel rule-trace"><h3>Regras consultadas</h3><p class="note">Nenhuma resolução mecânica foi executada nesta cena ainda.</p></div>`;const ev=t.evidence||[];return `<details class="panel rule-trace" open><summary><b>Regras consultadas nesta ação</b> <span class="trace-status ${esc(String(t.status||'').toLowerCase())}">${esc(traceStatusLabel(t.status))}</span></summary>${ev.length?`<div class="trace-list">${ev.map(e=>`<button class="trace-row" data-rule-id="${esc(e.ruleId)}"><b>${esc(e.ruleId)}</b><span>${esc(e.source||'Fonte')} · ${esc(e.location||'')}</span></button>`).join('')}</div>`:'<p class="note">Nenhuma regra mecânica específica foi aplicada; a resposta ficou no âmbito narrativo.</p>'}${t.note?`<p class="trace-note">${esc(t.note)}</p>`:''}</details>`}
function showRuleDetail(p){const t=p.trace||{};const ev=t.evidence||[];const msg=ev.length?`${p.ruleId}: ${ev.map(e=>`${e.source} — ${e.location}`).join(' | ')}`:`${p.ruleId}: ${traceStatusLabel(t.status)}`;showToast(msg)}

function renderPrestart(){return `<div class="kicker">Pronto para começar</div><div class="panel prestart-card"><span class="pill">${profileLabel()}</span><h2>A aventura ainda não começou</h2><p>Seu personagem e o mundo já estão preparados. Quando você tocar no botão abaixo, Barbara apresentará a cena inicial usando o estado atual da campanha.</p><button class="primary start-adventure" data-start-adventure>COMEÇAR A AVENTURA</button><button class="secondary" style="margin-top:7px" data-go="dice">ROLAR DADOS</button></div>`}
function renderCreationStart(){return `<div class="kicker">Novo aventureiro</div><div class="panel"><h2>Criar personagem</h2><p>Os seis atributos serão rolados pelo próprio aplicativo conforme as regras OSE e ficarão registrados na campanha.</p><span class="pill">${profileLabel()}</span><button class="primary" style="margin-top:10px" data-start-draft>CRIAR PASSO A PASSO · ROLAR 3D6</button><button class="secondary" style="margin-top:7px" data-random-character>CRIAR PERSONAGEM ALEATÓRIO</button><button class="secondary" style="margin-top:7px" data-import-character>IMPORTAR FICHA.JSON</button></div>`}
function attrMod(val){
  if(val==null)return'';
  if(val>=18)return'+3';
  if(val>=16)return'+2';
  if(val>=13)return'+1';
  if(val>=9)return'0';
  if(val>=6)return'-1';
  if(val>=4)return'-2';
  return'-3';
}

function classRecommendation(a, isAdv){
  const s = a.str||10, i = a.int||10, w = a.wis||10, d = a.dex||10, c = a.con||10, ch = a.cha||10;
  const maxVal = Math.max(s, i, w, d, c, ch);
  if(s === maxVal) return { prime: 'Força (FOR)', rec: 'Guerreiro', tip: 'Excelente capacidade de combate corpo a corpo e maior bônus de dano.' };
  if(w === maxVal) return { prime: 'Sabedoria (SAB)', rec: 'Clérigo', tip: 'Alta proteção contra magias e habilidades divinas para curar o grupo.' };
  if(i === maxVal) return { prime: 'Inteligência (INT)', rec: 'Mago', tip: 'Poderosos feitiços arcanos; ganha línguas adicionais.' };
  if(d === maxVal) return { prime: 'Destreza (DES)', rec: 'Ladrão', tip: 'Bônus na CA, ataques à distância e perícias de ladinagem.' };
  if(c === maxVal) return { prime: 'Constituição (CON)', rec: isAdv?'Bárbaro / Guerreiro':'Anão / Guerreiro', tip: 'Pontos de vida adicionais e resistência física superior.' };
  return { prime: 'Carisma (CAR)', rec: isAdv?'Bardo / Cavaleiro':'Guerreiro ou Clérigo', tip: 'Maior lealdade e reação favorável de contratados e monstros.' };
}

function renderCreationDraft(d){
 const a=d.attributes||{};const isAdv=String(d.profile||'').includes('ADVANCED');
 const classes=isAdv?['ACROBAT','ASSASSIN','BARBARIAN','BARD','CLERIC','DRUID','FIGHTER','ILLUSIONIST','KNIGHT','MAGIC_USER','PALADIN','RANGER','THIEF']:['DWARF','CLERIC','ELF','FIGHTER','HALFLING','THIEF','MAGIC_USER'];
 const races=['HUMAN','DROW','DUERGAR','DWARF','ELF','GNOME','HALF_ELF','HALFLING','HALF_ORC','SVIRFNEBLIN'];
 const rec = classRecommendation(a, isAdv);
 const classComp = [
   { id: 'FIGHTER', name: 'Guerreiro', prime: 'FOR', hd: 'd8', arm: 'Todas', desc: 'Especialista em armas e combate direto.' },
   { id: 'CLERIC', name: 'Clérigo', prime: 'SAB', hd: 'd6', arm: 'Todas + Escudos', desc: 'Magias divinas, cura e expulsão de mortos-vivos.' },
   { id: 'MAGIC_USER', name: 'Mago', prime: 'INT', hd: 'd4', arm: 'Nenhuma', desc: 'Conjura feitiços arcanos poderosos de grimório.' },
   { id: 'THIEF', name: 'Ladrão', prime: 'DES', hd: 'd4', arm: 'Couro apenas', desc: 'Furtividade, arrombamento e ataque pelas costas.' }
 ];
 return `<div class="kicker">Novo aventureiro · Aprender Jogando</div>
 <div class="panel">
   <h3>Criar personagem</h3>
   <p class="note">Atributos rolados com 3d6 estritos em ordem. Requisitos primários de 13+ concedem bônus de +5% de XP (+10% para 16+).</p>
   <div class="attrs">${Object.entries({FOR:a.str,INT:a.int,SAB:a.wis,DES:a.dex,CON:a.con,CAR:a.cha}).map(([k,v])=>`<div class="attr" title="Modificador: ${attrMod(v)}"><b>${v??'—'}</b><span>${k} (${attrMod(v)})</span></div>`).join('')}</div>
   <div class="guide-card" style="margin-top:10px;border-left:3px solid #d9a84c;background:rgba(217,168,76,0.08);padding:8px 12px">
     <b style="color:#d9a84c">💡 Recomendação OSE:</b> Sua melhor aptidão é <b>${esc(rec.prime)}</b> (${Math.max(a.str||0,a.int||0,a.wis||0,a.dex||0,a.con||0,a.cha||0)}).
     <span style="display:block;margin-top:2px;font-size:12px;color:#cbb898">Classe sugerida: <b>${esc(rec.rec)}</b>. ${esc(rec.tip)}</span>
   </div>
   <div style="margin-top:10px">
     <h4 style="margin-bottom:6px">Comparação rápida de classes clássicas:</h4>
     <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:6px;font-size:11px">
       ${classComp.map(c=>`<div style="background:rgba(0,0,0,0.25);padding:6px;border-radius:4px"><b>${c.name}</b> (${c.prime})<br>DV: ${c.hd} · Armaduras: ${c.arm}<br><span style="color:#a89880">${c.desc}</span></div>`).join('')}
     </div>
   </div>
   ${isAdv?`<label class="field" style="margin-top:10px">Método de criação</label><select id="creationMethod"><option value="ADVANCED_BASIC">Básico</option><option value="ADVANCED_METHOD">Avançado</option></select><label class="field">Raça (método avançado)</label><select id="creationRace"><option value="">Nenhuma / conforme método</option>${races.map(x=>`<option value="${x}">${raceLabel(x)}</option>`).join('')}</select>`:''}
   <label class="field" style="margin-top:10px">Classe</label><select id="creationClass">${classes.map(x=>`<option value="${x}">${classLabel(x)}</option>`).join('')}</select>
   <label class="field">Nome do aventureiro</label><input id="creationName" placeholder="Ex.: Rogar, Morwen, Alistair">
   <button class="primary" style="margin-top:9px" data-commit-draft="${esc(d.draftId)}">Criar personagem</button>
   <button class="secondary" style="margin-top:7px;width:100%" data-cancel-draft="${esc(d.draftId)}">CANCELAR CRIAÇÃO</button>
 </div>`;
}

const geom=Object.freeze({hexW:64,hexH:73.90,xStep:62.8,yStep:54.8,oddOffset:31.4,originX:24,originY:28});
function terrainAsset(c){if(!c||!c.known||c.terrain==='UNKNOWN')return null;if(c.hydrology==='SEA_OCEAN'||c.hydrology==='LAKE_RIVER')return 'agua.png';const m={CLEAR_GRASSLANDS:'planicie.png',HILLS:'colinas.png',MOUNTAINS:'montanha.png',FOREST:'floresta.png',DESERT:'planicie.png',SWAMP:'pantano.png',JUNGLE:'floresta.png',BARREN_ADV:'planicie.png',BROKEN_LANDS_ADV:'colinas.png'};return m[c.terrain]||'planicie.png'}
function terrainName(c){if(!c||!c.known||c.terrain==='UNKNOWN')return 'Área desconhecida';if(c.hydrology==='SEA_OCEAN')return 'Mar';if(c.hydrology==='LAKE_RIVER')return 'Rio ou lago';return terrainLabels[c.terrain]||'Terreno'}
function currentMapCell(){const p=viewState?.position,m=viewState?.map?.cells||[];return m.find(c=>c.q===p?.q&&c.r===p?.r)||null}
function renderHexMap(){
 const cells=viewState?.map?.cells||[];const pos=viewState?.position||{q:0,r:0};const selected=mapSelection?cells.find(c=>c.q===mapSelection.q&&c.r===mapSelection.r):currentMapCell();
 const width=viewState?.map?.width||6,height=viewState?.map?.height||5;
 const cellHtml=c=>{const x=geom.originX+c.q*geom.xStep+(c.r%2?geom.oddOffset:0),y=geom.originY+c.r*geom.yStep,a=terrainAsset(c),sel=selected&&selected.q===c.q&&selected.r===c.r,dq=c.q-pos.q,dr=c.r-pos.r,frontier=!c.known&&['1,0','0,1','1,-1','-1,0','-1,1','0,-1'].includes(`${dq},${dr}`);return `<button class="hex-cell ${c.known?'known':'unknown'} ${frontier?'frontier':''} ${sel?'selected':''}" data-map-q="${c.q}" data-map-r="${c.r}" aria-label="${esc(terrainName(c))}" style="left:${x}px;top:${y}px;${a?`background-image:url('hexes-runtime/${a}')`:''}">${frontier?'·':''}</button>`};
 const all=(cells.length?cells:Array.from({length:30},(_,i)=>({q:i%6,r:Math.floor(i/6),known:false,terrain:'UNKNOWN',hydrology:'UNKNOWN'}))).map(cellHtml).join('');
 const pq=Number.isFinite(pos.q)?pos.q:0,pr=Number.isFinite(pos.r)?pos.r:0,px=geom.originX+pq*geom.xStep+(pr%2?geom.oddOffset:0)+16,py=geom.originY+pr*geom.yStep+17;
 const travel=selected?travelForSelection(pos,selected):null;
 const gridW=Math.ceil(geom.originX*2+(Math.max(1,width)-1)*geom.xStep+geom.oddOffset+geom.hexW+92),gridH=Math.ceil(geom.originY*2+(Math.max(1,height)-1)*geom.yStep+geom.hexH+28);
 const knownText=selected?.known?`Terreno conhecido: ${terrainName(selected)}.`:'Esta área ainda não foi revelada à companhia.';
 return `<section class="map-shell"><div class="map-head"><div><div class="kicker">Mapa da campanha</div><div class="map-title">Ermos do Norte</div></div>${mapModeTabs()}<div class="camera-tools"><button id="zoomOut">−</button><button id="zoomIn">+</button></div></div><div id="mapViewport" class="map-viewport" aria-label="Mapa da campanha"><div id="mapGrid" class="map-grid" style="width:${gridW}px;height:${gridH}px">${all}<div class="canonical-ring" style="left:${px-9}px;top:${py-9}px"></div><div class="party-marker" style="left:${px}px;top:${py}px" aria-label="Companhia">◆</div><div class="map-label" style="left:${px+30}px;top:${py+2}px">COMPANHIA</div></div></div><div class="map-inspect panel"><div>${selected&&terrainAsset(selected)?img('hexes-runtime/'+terrainAsset(selected),terrainName(selected),'inspect-hex'):'<div class="unknown-inspect">✦</div>'}</div><div><div class="inspect-title">${esc(selected?terrainName(selected):'Selecione um hex')}</div><div class="inspect-copy">${esc(knownText)}</div>${travel?`<button class="small map-travel" data-map-travel="${esc(travel.intent)}" data-map-text="${esc(travel.text)}">Preparar dia de viagem para ${travel.label}</button>`:'<div class="inspect-copy">Selecione um hex vizinho à companhia para preparar a viagem.</div>'}</div></div></section>`
}

function effectiveMapMode(){
 const kind=String(viewState?.position?.kind||'HEX');
 if(mapMode!=='AUTO')return mapMode;
 return kind==='SETTLEMENT'?'SETTLEMENT':kind==='DUNGEON'?'DUNGEON':'HEX';
}
function mapModeTabs(){
 const lm=viewState?.localMaps||{},mode=effectiveMapMode();
 const tabs=[['HEX','ERMOS',true],['SETTLEMENT','CIDADE',!!lm.settlement],['DUNGEON','DUNGEON',!!lm.dungeon]].filter(x=>x[2]);
 return '<div class="map-mode-tabs">'+tabs.map(([id,label])=>'<button class="small '+(mode===id?'active':'')+'" data-map-mode="'+id+'">'+label+'</button>').join('')+'</div>';
}
function generatedMapAsset(key){return (window.OSE_GENERATED_MAP_ASSETS||{})[key]||''}
function localSprite(key,label,cls=''){
 const src=generatedMapAsset(key);
 return src?img(src,label,'local-map-sprite '+cls):'<div class="local-map-sprite placeholder">✦</div>';
}
function renderMap(){
 const mode=effectiveMapMode(),lm=viewState?.localMaps||{};
 if(mode==='SETTLEMENT'&&lm.settlement)return renderSettlementMap(lm.settlement);
 if(mode==='DUNGEON'&&lm.dungeon)return renderDungeonMap(lm.dungeon);
 return renderHexMap();
}
function settlementVisualKey(id){
 const m={GATE:'settlement_gate',INN:'settlement_inn',TEMPLE:'settlement_temple',SMITH:'settlement_smith',MARKET:'settlement_market'};
 return m[id]||'settlement_inn';
}
function renderSettlementMap(s){
 const slots={GATE:[50,86],INN:[24,32],MARKET:[50,42],SMITH:[77,34],TEMPLE:[73,72]};
 const places=(s.locations||[]).filter(x=>x.discovered);
 const sprites=places.map((p,i)=>{
   const xy=slots[p.locationId]||[20+(i%3)*30,25+Math.floor(i/3)*40];
   const current=p.locationId===s.currentAnchorId;
   return '<div class="settlement-place '+(current?'current':'')+'" style="left:'+xy[0]+'%;top:'+xy[1]+'%">'+
     localSprite(settlementVisualKey(p.locationId),p.name)+
     '<span>'+esc(p.name)+'</span>'+(current?'<b>◆</b>':'')+'</div>';
 }).join('');
 return '<section class="map-shell local-map-shell">'+
   '<div class="map-head"><div><div class="kicker">Mapa local</div><div class="map-title">Assentamento</div></div>'+mapModeTabs()+'</div>'+
   '<div class="settlement-map-board"><div class="town-road road-a"></div><div class="town-road road-b"></div><div class="town-road road-c"></div>'+sprites+'</div>'+
   '<div class="panel local-map-note"><b>'+esc(s.settlementId||'Assentamento')+'</b><p>Locais descobertos: '+places.length+'. O mapa é composto a partir dos sprites top-down incorporados e respeita apenas locais já revelados no estado canônico.</p></div>'+
   '</section>';
}
function dungeonPositions(nodes){
 const pts=[[50,50],[30,50],[70,50],[50,28],[50,72],[24,27],[76,27],[24,73],[76,73],[12,50],[88,50],[50,12],[50,88],[12,25],[88,25],[12,75],[88,75]];
 const out={};(nodes||[]).forEach((n,i)=>out[n.nodeId]=pts[i%pts.length]);return out;
}
function dungeonSpriteKey(node,i){
 if(node.nodeId===viewState?.localMaps?.dungeon?.entranceNodeId)return 'dungeon_ritual';
 const keys=['dungeon_room','dungeon_corridor','dungeon_cave','dungeon_water'];
 return keys[i%keys.length];
}
function renderDungeonMap(d){
 const nodes=d.nodes||[],edges=d.edges||[],pos=dungeonPositions(nodes);
 const lines=edges.map(e=>{const a=pos[e.from],b=pos[e.to];if(!a||!b)return'';return '<line x1="'+a[0]+'%" y1="'+a[1]+'%" x2="'+b[0]+'%" y2="'+b[1]+'%" class="'+(e.kind==='DOOR'?'door':'passage')+'"/>';}).join('');
 const rooms=nodes.map((n,i)=>{const p=pos[n.nodeId]||[50,50],current=n.current||n.nodeId===d.currentNodeId;return '<div class="dungeon-node '+(current?'current':'')+'" style="left:'+p[0]+'%;top:'+p[1]+'%">'+localSprite(dungeonSpriteKey(n,i),n.nodeId)+(current?'<b>◆</b>':'')+'<span>'+esc(n.nodeId)+'</span></div>';}).join('');
 return '<section class="map-shell local-map-shell">'+
   '<div class="map-head"><div><div class="kicker">Mapa interno</div><div class="map-title">Dungeon</div></div>'+mapModeTabs()+'</div>'+
   '<div class="dungeon-map-board"><svg class="dungeon-links" viewBox="0 0 100 100" preserveAspectRatio="none">'+lines+'</svg>'+rooms+'</div>'+
   '<div class="panel local-map-note"><b>'+esc(d.dungeonId||'Dungeon')+'</b><p>Áreas reveladas: '+nodes.length+'. Salas secretas ou ainda não conhecidas não são projetadas para a interface.</p></div>'+
   '</section>';
}

function travelForSelection(pos,c){if(pos?.kind!=='HEX')return null;const dq=c.q-pos.q,dr=c.r-pos.r;const m={'1,0':['leste','WILD_TRAVEL_DAY:E'],'0,1':['sudeste','WILD_TRAVEL_DAY:SE'],'1,-1':['nordeste','WILD_TRAVEL_DAY:NE'],'-1,0':['oeste','WILD_TRAVEL_DAY:W'],'-1,1':['sudoeste','WILD_TRAVEL_DAY:SW'],'0,-1':['noroeste','WILD_TRAVEL_DAY:NW']};const x=m[`${dq},${dr}`];return x?{label:x[0],intent:x[1],text:`Viajamos para ${x[0]} com cautela, observando o terreno durante o caminho.`}:null}

function hpText(c){if(!c)return'—';const d=c.hpDenominator||1,cur=c.hpCurrentNumerator??0,max=c.hpMaxNumerator??0;return d===1?`${cur}/${max}`:`${cur}/${d} / ${max}/${d}`}
function attrPairs(a={}){return [['FOR',a.str],['INT',a.int],['SAB',a.wis],['DES',a.dex],['CON',a.con],['CAR',a.cha]]}
function canRecreateCharacter(){return !!viewState?.selectedCharacter && !String(viewState?.session?.visibleNarration||'').trim() && Number(viewState?.time?.turns||0)===0}
function renderSheet(){
 const c=viewState?.selectedCharacter;
 if(hasNative()&&(creatingAdditionalCharacter||!c)){
  const draft=(viewState?.creation?.drafts||[])[0];
  return `<div class="kicker">Ficha do personagem</div>${(viewState?.party||[]).length?'<div class="panel"><button class="secondary" data-cancel-add-character>VOLTAR À FICHA</button></div>':''}${draft?renderCreationDraft(draft):renderCreationStart()}`;
 }
 const demo=c||{characterId:'demo-character',name:'Aventureiro',raceId:'HUMAN',classIds:['FIGHTER'],levelByClass:{FIGHTER:1},hpCurrentNumerator:8,hpMaxNumerator:8,hpDenominator:1,armorClassDescending:9,attributes:{str:11,int:9,wis:8,dex:10,con:6,cha:9}};
 const inv=(viewState?.inventory||(!hasNative()?[{itemId:'demo-mace',catalogId:'ADV_EQUIP_WEAPON_PURCHASE_09',quantity:1,equipped:false,kind:'WEAPON'}]:[])).filter(it=>!hasNative()||it.ownerId===demo.characterId);
 const editPanel=editingCharacter?`<div class="panel edit-character"><h3>Editar ficha</h3><label class="field">Nome do personagem</label><input id="editCharacterName" maxlength="80" value="${esc(demo.name||'')}"><p class="note">O nome pode ser alterado a qualquer momento. Raça, classe, atributos, HP inicial e valores derivados continuam protegidos pelas regras OSE. Antes da primeira cena, use “Criar outro personagem” para refazer a parte mecânica.</p><div class="row"><button class="primary" data-save-character>Salvar</button><button class="secondary" data-cancel-character-edit>Cancelar</button></div></div>`:'';
 const manage=hasNative()?`${renderCharacterControlSelect({...demo,controlMode:(viewState?.characterControlModes||{})[demo.characterId]||"PLAYER"})}<button class="secondary" data-add-character>ADICIONAR PERSONAGEM</button><button class="secondary" data-delete-character="${esc(demo.characterId)}">EXCLUIR FICHA</button>`:'';
 const portraitUrl=viewState?.presentation?.characterPortraits?.[demo.characterId];
  const portraitHtml=`<div class="portrait">${portraitUrl?`<img src="${esc(portraitUrl)}" alt="Retrato">`:"👤"}${hasNative()?`<button class="small photo-action" data-photo-character="${esc(demo.characterId)}" title="Alterar retrato">📷</button>`:""}</div>`;
  return `<div class="hero panel text-first-hero">${portraitHtml}<div><div class="kicker">Personagem</div><h2>${esc(demo.name||'Aventureiro')}</h2><p><b>${esc(raceLabel(demo.raceId||'HUMAN'))}</b> · ${esc((demo.classIds||[]).map(classLabel).join(' / ')||'—')} · ${profileLabel()}</p><div class="vitals">
  <div class="vital" data-guide-topic="hit_points" title="Pontos de Vida (HP) · Toque para ajuda"><span>HP <small class="help-badge">?</small></span><b>${hpText(demo)}</b></div>
  <div class="vital" data-guide-topic="armor_class" title="Classe de Armadura (CA) descendente · Toque para ajuda"><span>CA <small class="help-badge">?</small></span><b>${demo.armorClassDescending??'—'}</b></div>
  <div class="vital" data-guide-topic="character_advancement" title="Nível de experiência · Toque para ajuda"><span>NÍVEL <small class="help-badge">?</small></span><b>${Object.values(demo.levelByClass||{})[0]??1}</b></div>
</div></div></div>
 ${editPanel}
 <div class="panel">
  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
    <h3 style="margin:0">Jogadas de Proteção</h3>
    <button class="small" data-guide-topic="saving_throws" style="padding:2px 8px;font-size:11px">? COMO FUNCIONA</button>
  </div>
  <div class="attrs" style="grid-template-columns:repeat(auto-fit,minmax(60px,1fr));margin-bottom:12px">
    <div class="attr" title="Morte / Veneno"><b>${demo.savingThrows?.deathPoison??'—'}</b><span>Morte</span></div>
    <div class="attr" title="Varinhas mágicas"><b>${demo.savingThrows?.wands??'—'}</b><span>Varinhas</span></div>
    <div class="attr" title="Paralisia / Petrificação"><b>${demo.savingThrows?.paralysisPetrification??'—'}</b><span>Paralisia</span></div>
    <div class="attr" title="Sopro de dragão"><b>${demo.savingThrows?.breath??'—'}</b><span>Sopro</span></div>
    <div class="attr" title="Magias / Cajados"><b>${demo.savingThrows?.spellsRodsStaves??'—'}</b><span>Magias</span></div>
  </div>
  <div class="attrs">${attrPairs(demo.attributes).map(([k,v])=>`<div class="attr" title="Modificador: ${attrMod(v)}"><b>${v??'—'}</b><span>${k} (${attrMod(v)})</span></div>`).join('')}</div><div class="sheet-actions"><button class="small" data-edit-character>EDITAR FICHA</button>${manage}<button class="small" data-export-character>EXPORTAR FICHA.JSON</button><button class="small" data-import-character>IMPORTAR FICHA.JSON</button></div></div>
 <div class="panel"><h3>Inventário</h3><div class="inventory-grid">${inv.length?inv.map(renderInventoryItem).join(''):'<p class="note">Inventário vazio.</p>'}</div></div>${hasNative()?renderNativeLoadout():renderDemoShop()}`;
}
function loadoutState(){return viewState?.loadout&&viewState.loadout.characterId?viewState.loadout:null}
function catalogEntry(id){return loadoutState()?.catalog?.find(x=>x.catalogId===id)||null}
function catalogLabel(id,item){if(id==='STARTING_GOLD_GP')return 'Moedas de ouro';return String(item?.displayName||item?.name||catalogEntry(id)?.name||id||'Equipamento sem identificação')}
function itemArt(catalogId,name='',kind=''){if(/^(CLASSIC|ADV)_EQUIP_/.test(String(catalogId||'')))return img(`equipment-icons/${esc(catalogId)}.png`,name,'item-thumb equipment-art');const p=kind==='SHIELD'?'tesouros_escudos/escudo_redondo.png':kind==='ARMOR'?'equipamentos/cota_malha.png':'equipamentos_variantes/equipamentos_variantes_01.png';return img(asset(p),name,'item-thumb')}
function renderInventoryItem(it){const name=catalogLabel(it.catalogId,it),custom=viewState?.presentation?.itemThumbnails?.[it.itemId],canEquip=['WEAPON','ARMOR','SHIELD'].includes(it.kind)&&loadoutState()&&!loadoutState().completed;return `<div class="item">${custom?img(custom,name,'item-thumb'):itemArt(it.catalogId,name,it.kind)}<b>${esc(name)}</b><small>×${it.quantity??1}${it.equipped?' · EQUIPADO':''}</small>${hasNative()?`<div class="actions">${canEquip?`<button data-loadout-equip="${esc(it.itemId)}" data-equipped="${it.equipped?'1':'0'}">${it.equipped?'TIRAR':'EQUIPAR'}</button>`:''}<button class="photo-action" title="Trocar imagem" data-photo-item="${esc(it.itemId)}">📷</button></div>`:''}</div>`}
function renderNativeLoadout(){const l=loadoutState();if(!hasNative()||!l||l.completed)return'';const groups=[['Aventura',['ADVENTURING_GEAR','FOOD','LIGHT_SOURCE','OIL_FLASK']],['Armas',['WEAPON']],['Armaduras e munição',['ARMOR','SHIELD','AMMUNITION']]];return `<div class="panel"><div class="kicker">Equipamento inicial</div><h3>${l.remainingWealthGp} po restantes de ${l.startingWealthGp} po</h3><p class="note">Os preços, quantidades e estatísticas são aplicados automaticamente pelas regras OSE.</p>${groups.map(([title,kinds])=>`<h3>${title}</h3><div class="inventory-grid">${(l.catalog||[]).filter(e=>kinds.includes(e.kind)).map(e=>`<div class="item">${itemArt(e.catalogId,e.name,e.kind)}<b>${esc(e.name)}</b><small>${e.priceGp==null?'Preço não comprovado':`${e.priceGp} po`}${e.purchaseQuantity>1?` · ×${e.purchaseQuantity}`:''}${e.ownedQuantity?` · possui ${e.ownedQuantity}`:''}</small><div class="actions"><button data-loadout-buy="${esc(e.catalogId)}" ${e.priceGp==null||e.priceGp>l.remainingWealthGp?'disabled':''}>COMPRAR</button></div></div>`).join('')}</div>`).join('')}<button class="primary" data-loadout-finish="1" style="margin-top:9px">Concluir equipamento</button><button class="secondary" data-loadout-back="1" style="margin-top:7px;width:100%">VOLTAR À FICHA (MANTER PERSONAGEM)</button></div>`}
function renderDemoShop(){return ''}
function renderCharacterControlSelect(m){
 const mode=m.controlMode||'PLAYER';
 const options=[['PLAYER','Eu controlo'],['MASTER','Mestre controla'],['CURATED','Mestre + minha curadoria']];
 return '<label class="character-control-label">Controle do personagem<select data-character-mode="'+esc(m.characterId)+'" aria-label="Controle de '+esc(m.name)+'">'+options.map(([v,label])=>'<option value="'+v+'"'+(v===mode?' selected':'')+'>'+label+'</option>').join('')+'</select></label>';
}
function renderCompanionOpinions(){
 const hasCompany=(viewState?.party||[]).some(m=>m.controlMode==='MASTER'||m.controlMode==='CURATED');
 if(!hasCompany)return '';
 const currentHash=viewState?.campaign?.mechanicalStateHash;
 const preview=companionOpinions&&(!currentHash||companionOpinions.stateHash===currentHash)
   ?companionOpinions.opinions||[]:null;
 const label={SUPPORT:'Concorda',CAUTION:'Pede cautela',OBJECT:'Discorda',UNCERTAIN:'Tem dúvidas'};
 return '<div class="panel companion-opinions"><button class="secondary" data-companion-opinions>O QUE OS COMPANHEIROS ACHAM?</button>'+
 '<p class="note">Consulta sem rolagens, tempo ou avanço da cena. Opiniões podem estar erradas.</p>'+
 (preview===null?'':preview.length===0?'<p>Nenhum companheiro controlado pelo Mestre.</p>':
  preview.map(x=>'<div class="companion-opinion"><b>'+esc(x.name)+'</b> · '+esc(label[x.stance]||'Opinião')+
   '<p><b>Percepção:</b> '+esc(x.perception)+'</p><p><b>Hipótese:</b> '+esc(x.hypothesis)+
   '</p><p><b>Motivação:</b> '+esc(x.motivation)+'</p><p><b>Sugestão:</b> '+esc(x.suggestion)+
   '</p><p><b>Risco:</b> '+esc(x.risk)+'</p><p><b>Motivo:</b> '+esc(x.reason)+'</p></div>').join(''))+'</div>';
}
function renderCuratorPanel(){
 const pending=viewState?.pendingCharacterActions||[];
 if(!pending.length)return '';
 return '<div class="panel curator-panel"><h3>Aprovação de ações do Mestre</h3><p class="note">A proposta não produz consequências antes da resolução autorizada.</p>'+
 pending.map(p=>'<div class="curator-entry"><b>'+esc(p.characterName)+'</b>'+
 '<p><b>Percepção:</b> '+esc(p.perception||'Não detalhada')+'</p>'+
 '<p><b>Hipótese:</b> '+esc(p.hypothesis||'Incerta')+'</p>'+
 '<p><b>Motivação:</b> '+esc(p.motivation||'Ajudar o grupo')+'</p>'+
 '<p><b>Motivo:</b> '+esc(p.reason||'Ainda não explicitado')+'</p>'+
 '<p><b>Risco:</b> '+esc(p.risk||'Indeterminado')+'</p>'+
 '<label>PROPOSTA / ALTERAR AÇÃO<textarea data-curator-text="'+esc(p.proposalId)+'" rows="3">'+esc(p.text)+'</textarea></label>'+
 '<div class="character-actions"><button class="primary" data-curator-approve="'+esc(p.proposalId)+'">APROVAR</button>'+
 '<button class="secondary" data-curator-change="'+esc(p.proposalId)+'">ALTERAR E APROVAR</button>'+
 '<button class="secondary" data-curator-reject="'+esc(p.proposalId)+'">RECUSAR</button></div></div>').join('')+'</div>';
}
function renderMasterContinue(){
 const controlled=(viewState?.party||[]).filter(m=>m.controlMode==='MASTER'||m.controlMode==='CURATED');
 return controlled.length?'<div class="panel"><h3>Personagens do Mestre</h3><p class="note">O Mestre age automaticamente após suas ações. Também é possível solicitar uma rodada de decisões.</p><button class="secondary" data-master-continue>CONTINUAR AÇÕES DO MESTRE</button></div>':'';
}
function renderRetainerWage(r, actor) {
 const dueDays=Number(r.wageDueDays||0), dueGp=Number(r.wageDueGp||0);
 const note=r.wageLedgerNeedsBaseline ? 'Histórico antigo precisa ser reconciliado pelo árbitro'
  : 'Pago: '+Number(r.totalWagesPaidGp||0)+' po; pendente: '+dueGp+' po ('+dueDays+' dias)';
 const button=r.employerCharacterId!==actor ? '' : r.wageLedgerNeedsBaseline
  ? '<button class="secondary" data-retainer-baseline="'+esc(r.npcId)+'">RECONCILIAR HISTÓRICO</button>'
  : dueDays>0 ? '<button class="secondary" data-retainer-pay="'+esc(r.npcId)+'">PAGAR DIÁRIAS</button>' : '';
 return '<div class="note">'+esc(note)+'</div>'+button;
}

function renderRetainerPanel(){
 const hired=(viewState?.retainers||[]),candidates=(viewState?.retainerCandidates||[]),actor=viewState?.selectedCharacterId;
 const employed=hired.filter(x=>x.active);
 return `<div class="panel retainer-panel"><h3>Auxiliares de aventura</h3>
 <p class="note">O recrutamento usa 2d6 e o Carisma do contratante. Só são elegíveis NPCs com nível, classe e ficha mecânica comprovados. Mercenários militares não são auxiliares.</p>
 ${employed.length?`<div class="text-entity-list">${employed.map(r=>`<div class="text-entity"><b>${esc(r.name)}</b><span>Empregador: ${esc((viewState?.party||[]).find(p=>p.characterId===r.employerCharacterId)?.name||r.employerCharacterId)} · ${r.dailyWageGp} po/dia · ${r.treasureHalfShares}/2 cota(s) de tesouro</span>${renderRetainerWage(r,actor)}</div>`).join('')}</div>`:'<p class="note">Nenhum auxiliar contratado.</p>'}
 ${hired.some(x=>!x.active)?'<p class="note">Auxiliares que encerraram o contrato continuam registrados no histórico.</p>':''}
 <h3>Propor contratação</h3>
 ${!actor?'<p class="note">Selecione primeiro um personagem.</p>':!candidates.length?'<p class="note">Nenhum NPC presente possui ficha de auxiliar comprovada.</p>':`
 <div class="row"><label class="field">Diária proposta (po)<input id="retainerDailyWage" type="number" min="0" step="1" value="1"></label>
 <label class="field">Meias partes do tesouro<input id="retainerTreasureHalves" type="number" min="1" step="1" value="1"></label></div>
 <div class="text-entity-list">${candidates.map(n=>`<div class="text-entity"><b>${esc(n.name)}</b><button class="secondary" data-retainer-proposal="${esc(n.npcId)}">FAZER OFERTA</button></div>`).join('')}</div>`}
 </div>`;
}

function renderCompany(){const party=viewState?.party||(!hasNative()?[{characterId:'demo-character',name:'Aventureiro',classIds:['FIGHTER'],hpCurrentNumerator:8,hpMaxNumerator:8,hpDenominator:1}]:[]);return `<div class="kicker">Companhia</div><div class="panel"><h2>${party.length?'Companhia atual':'Ainda vazia'}</h2>${party.length?`<div class="text-entity-list">${party.map(m=>{const mPort=viewState?.presentation?.characterPortraits?.[m.characterId];return `<div class="text-entity character-management ${m.characterId===viewState?.selectedCharacterId?'active':''}"><div>${mPort?`<img src="${esc(mPort)}" class="avatar-thumb" alt="Retrato">`:""}<b>${esc(m.name)}</b><span>${esc((m.classIds||[]).map(classLabel).join(' / '))} · HP ${hpText(m)} · Nível ${esc(Object.values(m.levelByClass||{})[0]||1)}</span></div>${renderCharacterControlSelect(m)}<div class="character-actions"><button data-character-select="${esc(m.characterId)}">ABRIR FICHA</button>${hasNative()?`<button class="secondary photo-action" data-photo-character="${esc(m.characterId)}" title="Alterar retrato">📷</button>`:""}<button class="secondary" data-delete-character="${esc(m.characterId)}">EXCLUIR FICHA</button></div></div>`}).join('')}</div><p class="note">A companhia viaja e ocupa o mundo como um único grupo.</p>`:'<p>Crie seu primeiro personagem para formar a companhia.</p>'}<div class="character-actions"><button class="primary" data-add-character>ADICIONAR PERSONAGEM</button><button class="secondary" data-import-character>IMPORTAR FICHA.JSON</button></div></div>${renderCuratorPanel()}${renderCompanionOpinions()}${renderMasterContinue()}${renderRetainerPanel()}`}

function renderDicePanel(){return `<div class="panel dice-panel"><div class="row"><h3>🎲 Dados</h3><span class="note">Rolagem manual; não altera a campanha sozinha.</span></div><div class="dice-buttons">${[4,6,8,10,12,20,100].map(s=>`<button class="small" data-die="${s}">d${s}</button>`).join('')}</div><div class="dice-controls"><label>Quantidade<input id="diceCount" type="number" min="1" max="20" value="1"></label><label>Modificador<input id="diceModifier" type="number" min="-100" max="100" value="0"></label></div>${diceHistory.length?`<div class="dice-history">${diceHistory.map(d=>`<div><b>${d.count}d${d.sides}${d.modifier?`${d.modifier>0?'+':''}${d.modifier}`:''} = ${d.total}</b><small>${esc((d.rolls||[]).join(', '))}</small></div>`).join('')}</div>`:''}</div>`}
function renderDiceScreen(){return `<div class="kicker">Rolagem de dados</div>${renderDicePanel()}<div class="panel"><p class="note">Rolagens manuais são livres e não alteram a campanha sozinhas. Quando uma regra OSE exige um teste, o motor usa a rolagem autoritativa e registra a regra consultada na sessão.</p></div>`}
function renderLibrary(){const lib=viewState?.libraryStatus||{};const qx=libraryQuery.trim().toLowerCase();const filtered=ruleLibrary.filter(r=>!qx||String(r.ruleId).toLowerCase().includes(qx)||JSON.stringify(r.trace||{}).toLowerCase().includes(qx));return `<div class="kicker">Biblioteca de regras</div><div class="panel"><h2>${profileLabel()}</h2><p class="note">Autoridade: ${esc(lib.authority||'Fonte OSE incorporada')} · ${lib.ruleCount||ruleLibrary.length||0} regras catalogadas.</p><input id="librarySearch" type="search" placeholder="Buscar regra, fonte ou página" value="${esc(libraryQuery)}"></div><div class="rule-library">${ruleLibraryLoaded?(filtered.slice(0,100).map(r=>{const t=r.trace||{},e=(t.evidence||[])[0]||{};return `<button class="panel library-rule" data-rule-id="${esc(r.ruleId)}"><b>${esc(r.ruleId)}</b><span>${esc(traceStatusLabel(t.status))}</span><small>${esc(e.source||'')} ${esc(e.location||'')}</small></button>`}).join('')||'<div class="panel"><p>Nenhuma regra encontrada.</p></div>'):'<div class="panel"><p>Carregando catálogo de regras…</p></div>'}</div>`}
function renderWorld(){const w=viewState?.worldStatus||{},p=viewState?.position||{},cell=currentMapCell();const weather={CLEAR:'Céu limpo',OVERCAST:'Encoberto',RAIN:'Chuva',FOG:'Neblina',STORM:'Tempestade',COLD:'Frio intenso',HOT:'Calor forte'}[w.weather]||w.weather||'—';const rumors=window.vttLifeAdapter?.getPublicRumors()||[];const lifeArcs=window.vttLifeAdapter?.getActiveLifeArcs()||[];return `<div class="kicker">Mundo Vivo</div><div class="panel"><h2>${w.active?'Mundo ativo':'Mundo ainda não iniciado'}</h2><div class="world-grid"><div><span>Clima</span><b>${esc(weather)}</b></div><div><span>Pulsos autônomos</span><b>${w.worldPulseCount??0}</b></div><div><span>NPCs persistentes</span><b>${w.npcCount??0}</b></div><div><span>Atividade de NPCs</span><b>${w.npcActivityCount??0}</b></div><div><span>Hexes conhecidos</span><b>${w.knownHexes??0}</b></div><div><span>Eventos pendentes</span><b>${w.scheduledEventCount??0}</b></div><div><span>Eventos resolvidos</span><b>${w.resolvedEventCount??0}</b></div><div><span>Revisão do mundo</span><b>${w.revision??0}</b></div></div><p class="note">O mundo e as rotinas dos NPCs avançam automaticamente quando o tempo canônico passa. Clima e atividade de fundo são persistidos sem inventar modificadores de regra.</p></div><div class="panel"><h3>Posição atual</h3><p>${esc(cell?terrainName(cell):'Local atual')} · Dia ${(viewState?.time?.days??0)+1}, turno ${viewState?.time?.turns??0}</p><button class="secondary" data-go="map">ABRIR MAPA</button></div><div class="panel"><h3>Motor de Vida V2.2 · Rumores e Repercussões</h3>${rumors.length?`<ul class="rumor-list">${rumors.map(r=>`<li><b>Dia ${(r.day??0)+1}:</b> ${esc(r.text||r.title||'')}</li>`).join('')}</ul>`:'<p class="note">Nenhum rumor recente em circulação. Realize ações de viagem, descanso ou exploração para gerar manifestações biográficas.</p>'}</div>${renderRuleTrace(lastRuleTrace||viewState?.lastRuleTrace)}`}


function wireCurrentScreen(){
 document.querySelectorAll('[data-go]').forEach(b=>b.onclick=()=>go(b.dataset.go));
 document.querySelector('[data-start-adventure]')?.addEventListener('click',()=>{if(postNative('StartAdventure',{}))showToast('Barbara está preparando a cena inicial…')});
 document.querySelectorAll('[data-suggest]').forEach(b=>b.onclick=()=>{const t=el('reactionInput');if(t){t.value=b.dataset.suggest;t.dataset.mechanicalIntent=b.dataset.intent||'';t.focus()}});
 el('reactionInput')?.addEventListener('input',e=>{if(e.isTrusted)delete e.currentTarget.dataset.mechanicalIntent});
 document.querySelector('[data-react]')?.addEventListener('click',()=>{const t=el('reactionInput'),v=t?.value.trim();if(!v)return showToast('Escreva o que seu personagem faz ou pergunta.');const human=(viewState?.party||[]).find(m=>m.characterId===viewState?.selectedCharacterId&&(m.controlMode||'PLAYER')==='PLAYER')||(viewState?.party||[]).find(m=>(m.controlMode||'PLAYER')==='PLAYER');if(hasNative()&&!human)return showToast('Este personagem está sob controle do Mestre. Altere seu modo na Companhia.');const mechanicalIntent=t.dataset.mechanicalIntent||'';const execute=()=>{if(postNative('PlayerReaction',{reactionId:'ui-'+Date.now(),characterId:human?.characterId,text:v,mechanicalIntent}))showToast('Enviado ao Mestre.');else{localLog.push(v);showToast('Preview sem motor nativo.')}};const warning=actionCostSummary(mechanicalIntent);if(warning&&mechanicalIntent.startsWith('WILD_'))return confirmWildernessAction(warning,execute);execute()});
 document.querySelectorAll('[data-tts]').forEach(b=>b.onclick=()=>{if(!postNative('TtsCommand',{command:b.dataset.tts}))showToast('A narração em voz requer o app Android.');});
 document.querySelector('[data-scene-prompt]')?.addEventListener('click',()=>{if(postNative('ScenePromptShare',{explicitUserAction:true}))showToast('Abrindo o Gemini com o prompt da cena…');else showToast('Geração externa requer o app Android.')});
 document.querySelector('[data-scene-import]')?.addEventListener('click',b=>chooseMedia('SCENE',b.currentTarget.dataset.sceneId));
 document.querySelectorAll('[data-die]').forEach(b=>b.onclick=()=>{const count=Math.max(1,Math.min(20,Number(el('diceCount')?.value||1))),modifier=Math.max(-100,Math.min(100,Number(el('diceModifier')?.value||0)));if(!postNative('ManualDiceRoll',{sides:Number(b.dataset.die),count,modifier})){const rolls=Array.from({length:count},()=>1+Math.floor(Math.random()*Number(b.dataset.die))),total=rolls.reduce((a,x)=>a+x,0)+modifier;diceHistory.unshift({rolls,total,sides:Number(b.dataset.die),count,modifier});render()}});
 document.querySelector('[data-start-draft]')?.addEventListener('click',e=>{e.currentTarget.disabled=true;postNative('CharacterCreationStart',{draftId:'draft-'+Date.now()+'-'+Math.random().toString(36).slice(2,9)})});
 document.querySelector('[data-random-character]')?.addEventListener('click',e=>{e.currentTarget.disabled=true;if(postNative('CharacterCreationRandom',{commandId:'random-'+Date.now()+'-'+Math.random().toString(36).slice(2,9)}))showToast('Criando personagem aleatório pelas regras…');else e.currentTarget.disabled=false});
 document.querySelectorAll('[data-export-character]').forEach(b=>b.onclick=()=>postNative('CharacterSheetExportRequest',{characterId:b.dataset.exportCharacter||viewState?.selectedCharacterId}));
 document.querySelectorAll('[data-import-character]').forEach(b=>b.onclick=()=>postNative('CharacterSheetImportRequest',{}));
 document.querySelectorAll('[data-edit-character]').forEach(b=>b.onclick=()=>{editingCharacter=true;render()});
 document.querySelectorAll('[data-cancel-character-edit]').forEach(b=>b.onclick=()=>{editingCharacter=false;render()});
 document.querySelectorAll('[data-save-character]').forEach(b=>b.onclick=()=>{const name=el('editCharacterName')?.value.trim();if(!name)return showToast('Digite um nome válido.');b.disabled=true;postNative('CharacterSheetUpdate',{characterId:viewState?.selectedCharacterId,name,commandId:'edit-'+Date.now()+'-'+Math.random().toString(36).slice(2,9)})});
 document.querySelectorAll('[data-add-character]').forEach(b=>b.onclick=()=>{creatingAdditionalCharacter=true;editingCharacter=false;screen='sheet';render()});
  document.querySelectorAll('[data-cancel-add-character]').forEach(b=>b.onclick=()=>{creatingAdditionalCharacter=false;render()});
  document.querySelectorAll('[data-character-mode]').forEach(select=>select.onchange=()=>{const id=select.dataset.characterMode,mode=select.value;select.disabled=true;postNative('CharacterControlSet',{characterId:id,mode})});
 document.querySelectorAll('[data-curator-approve]').forEach(b=>b.onclick=()=>{const id=b.dataset.curatorApprove,p=(viewState?.pendingCharacterActions||[]).find(x=>x.proposalId===id);if(!p)return;const value=document.querySelector('[data-curator-text="'+id+'"]')?.value?.trim()||p.text;if(!value)return showToast('Descreva a ação antes de aprovar.');b.disabled=true;const edited=value!==p.text;postNative('MasterActionReview',{proposalId:id,approve:true,...(edited?{editedText:value,editedIntent:value}:{})})});
 document.querySelectorAll('[data-curator-change]').forEach(b=>b.onclick=()=>{
   const id=b.dataset.curatorChange,p=(viewState?.pendingCharacterActions||[]).find(x=>x.proposalId===id);
   if(!p)return;
   const edited=document.querySelector('[data-curator-text="'+id+'"]')?.value?.trim();
   if(!edited)return showToast('Descreva sua alternativa antes de aprovar.');
   if(edited===p.text)return showToast('Altere a proposta ou use APROVAR.');
   if(!confirm('Substituir a ação do Mestre pela sua versão e enviá-la ao motor de regras?'))return;
   b.disabled=true;
   postNative('MasterActionReview',{proposalId:id,approve:true,editedText:edited,editedIntent:edited});
 });
 document.querySelectorAll('[data-curator-reject]').forEach(b=>b.onclick=()=>{b.disabled=true;postNative('MasterActionReview',{proposalId:b.dataset.curatorReject,approve:false})});
 document.querySelectorAll('[data-companion-opinions]').forEach(b=>b.onclick=()=>{b.disabled=true;if(!postNative('CompanionOpinionsQuery',{}))b.disabled=false;});
 document.querySelectorAll('[data-master-continue]').forEach(b=>b.onclick=()=>{b.disabled=true;postNative('MasterActionContinue',{});showToast('Mestre escolhendo ações da companhia…')});
 document.querySelectorAll('[data-character-select]').forEach(b=>b.onclick=()=>{b.disabled=true;postNative('CharacterSelect',{characterId:b.dataset.characterSelect})});
  document.querySelectorAll('[data-delete-character]').forEach(b=>b.onclick=()=>{const id=b.dataset.deleteCharacter;const character=(viewState?.party||[]).find(c=>c.characterId===id);if(!character)return;if(!confirm('EXCLUIR A FICHA DE '+character.name+'?\n\nSeu inventário, equipamento e retrato deixarão de ser usados na companhia. Os demais personagens e o histórico da campanha serão preservados.'))return;b.disabled=true;postNative('CharacterDelete',{characterId:id,commandId:'delete-'+Date.now()+'-'+Math.random().toString(36).slice(2,9)})});
 document.querySelectorAll('[data-rule-id]').forEach(b=>b.onclick=()=>postNative('RuleEvidenceQuery',{ruleId:b.dataset.ruleId}));
 el('librarySearch')?.addEventListener('input',e=>{libraryQuery=e.currentTarget.value;render()});
 document.querySelector('[data-commit-draft]')?.addEventListener('click',e=>{const d=e.currentTarget.dataset.commitDraft,name=el('creationName')?.value.trim();if(!name)return showToast('Digite um nome.');e.currentTarget.disabled=true;const adv=String(viewState?.campaign?.profile||'').includes('ADVANCED');const method=adv?(el('creationMethod')?.value||'ADVANCED_BASIC'):'CLASSIC';const race=adv&&method==='ADVANCED_METHOD'?(el('creationRace')?.value||null):null;postNative('CharacterCreationCommit',{draftId:d,characterId:'char-'+Date.now()+'-'+Math.random().toString(36).slice(2,9),name,method,classIds:[el('creationClass').value],raceId:race||'',adjustments:[]})});
  document.querySelectorAll('[data-photo-character]').forEach(b=>b.onclick=()=>openPhotoMenu('CHARACTER',b.dataset.photoCharacter,'Personagem'));
  document.querySelectorAll('[data-photo-npc]').forEach(b=>b.onclick=()=>openPhotoMenu('NPC',b.dataset.photoNpc,'NPC'));
  document.querySelectorAll('[data-scene-photo]').forEach(b=>b.onclick=()=>openPhotoMenu('SCENE',b.dataset.sceneId,'Cena atual'));
  document.querySelectorAll('[data-adventure-import]').forEach(b=>b.onclick=()=>postNative('AdventureImport',{}));
  document.querySelectorAll('[data-adventure-finish]').forEach(b=>b.onclick=()=>{if(confirm('Concluir esta aventura e arquivar seus feitos? Seus personagens, itens, relações e mundo serão integralmente preservados.'))postNative('AdventureFinish',{})});
  document.querySelectorAll('[data-adventure-load-classic]').forEach(b=>b.onclick=()=>loadSampleAdventure('adventures/sample-adventure-ose-classic.json'));
  document.querySelectorAll('[data-adventure-load-advanced]').forEach(b=>b.onclick=()=>loadSampleAdventure('adventures/sample-adventure-ose-advanced.json'));

  document.querySelectorAll('[data-cancel-draft]').forEach(b=>b.onclick=e=>{
    const d=e.currentTarget.dataset.cancelDraft;
    creatingAdditionalCharacter=false;
    postNative('CharacterCreationCancel',{draftId:d});
  });
  document.querySelectorAll('[data-loadout-back]').forEach(b=>b.onclick=()=>{
    creatingAdditionalCharacter = false;
    go('sheet');
  });
  document.querySelectorAll('[data-photo-item]').forEach(b=>b.onclick=()=>openPhotoMenu('ITEM',b.dataset.photoItem,'Item'));
 document.querySelectorAll('[data-retainer-proposal]').forEach(b=>b.onclick=()=>{
   const npcId=b.dataset.retainerProposal,actor=viewState?.selectedCharacterId;
   const npc=(viewState?.retainerCandidates||[]).find(n=>n.npcId===npcId);
   const wage=Number(el('retainerDailyWage')?.value),halves=Number(el('retainerTreasureHalves')?.value);
   if(!npc||!actor||!Number.isSafeInteger(wage)||wage<0||!Number.isSafeInteger(halves)||halves<1)
      return showToast('Selecione um personagem e informe uma oferta válida.');
   b.disabled=true;
   const reactionId='retainer-offer-'+Date.now()+'-'+Math.random().toString(36).slice(2,9);
   const text='Ofereço a '+npc.name+' '+wage+' po por dia e '+halves+'/2 cota(s) de tesouro para nos acompanhar.';
   if(!postNative('PlayerReaction',{reactionId,text,mechanicalIntent:'RETAINER_RECRUIT:'+actor+':'+npcId+':'+wage+':'+halves}))b.disabled=false;
 });
 document.querySelectorAll('[data-retainer-baseline]').forEach(b=>b.onclick=()=>{
   const npcId=b.dataset.retainerBaseline,actor=viewState?.selectedCharacterId;
   if(!actor||!npcId||!confirm('Confirmar marco no dia atual? Dívidas antigas exigem arbitragem separada.'))return;
   b.disabled=true;
   const reactionId='retainer-baseline-'+Date.now()+'-'+Math.random().toString(36).slice(2,9);
   if(!postNative('PlayerReaction',{reactionId,characterId:actor,text:'Confirmo o marco de pagamento sem presumir dívidas passadas.',
     mechanicalIntent:'RETAINER_WAGE_BASELINE:'+actor+':'+npcId}))b.disabled=false;
 });
 document.querySelectorAll('[data-retainer-pay]').forEach(b=>b.onclick=()=>{
   const npcId=b.dataset.retainerPay,actor=viewState?.selectedCharacterId;
   const contract=(viewState?.retainers||[]).find(r=>r.npcId===npcId&&r.employerCharacterId===actor);
   if(!contract||!Number.isSafeInteger(Number(contract.wageDueGp))||
     !confirm('Pagar '+contract.wageDueGp+' po por '+contract.wageDueDays+' dia(s) com o ouro do contratante?'))return;
   b.disabled=true;
   const reactionId='retainer-pay-'+Date.now()+'-'+Math.random().toString(36).slice(2,9);
   if(!postNative('PlayerReaction',{reactionId,characterId:actor,text:'Pago as diárias vencidas de '+contract.name+'.',
     mechanicalIntent:'RETAINER_PAY_DAILY:'+actor+':'+npcId}))b.disabled=false;
 });
 document.querySelectorAll('[data-loadout-buy]').forEach(b=>b.onclick=()=>postNative('UiCommand',{command:'loadout_purchase',commandId:'buy-'+Date.now()+'-'+b.dataset.loadoutBuy,characterId:loadoutState()?.characterId||'',catalogId:b.dataset.loadoutBuy}));
 document.querySelectorAll('[data-loadout-equip]').forEach(b=>b.onclick=()=>postNative('UiCommand',{command:'loadout_equip',commandId:'equip-'+Date.now()+'-'+b.dataset.loadoutEquip,characterId:loadoutState()?.characterId||'',itemId:b.dataset.loadoutEquip,equipped:b.dataset.equipped!=='1'}));
 document.querySelector('[data-loadout-finish]')?.addEventListener('click',()=>postNative('UiCommand',{command:'loadout_finish',commandId:'finish-'+Date.now(),characterId:loadoutState()?.characterId||''}));
 if(screen==='map'){wireMapModeTabs();if(effectiveMapMode()==='HEX')wireMap();}
}
function wireMapModeTabs(){document.querySelectorAll('[data-map-mode]').forEach(b=>b.onclick=()=>{mapMode=b.dataset.mapMode||'AUTO';mapSelection=null;render()})}
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
function downscaleImage(file, maxDim, quality, callback) {
  const img = new Image();
  const url = URL.createObjectURL(file);
  img.onload = () => {
    URL.revokeObjectURL(url);
    let w = img.width, h = img.height;
    if (w > maxDim || h > maxDim) {
      if (w > h) { h = Math.round((h * maxDim) / w); w = maxDim; }
      else { w = Math.round((w * maxDim) / h); h = maxDim; }
    }
    const canvas = document.createElement('canvas');
    canvas.width = w; canvas.height = h;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(img, 0, 0, w, h);
    callback(file.type==='image/png'||file.type==='image/webp'?canvas.toDataURL('image/png'):canvas.toDataURL('image/jpeg',quality));
  };
  img.onerror = () => {
    URL.revokeObjectURL(url);
    showToast('Falha ao processar arquivo de imagem.');
  };
  img.src = url;
}
function chooseMedia(scope,id){mediaTarget={scope,id};el('mediaPicker').value='';el('mediaPicker').click()}
el('mediaPicker').addEventListener('change',()=>{
  const f=el('mediaPicker').files?.[0];
  if(!f||!mediaTarget)return;
  if(f.size>20*1024*1024)return showToast('Imagem deve ter no máximo 20 MB.');
  downscaleImage(f, 1024, 0.85, (dataUrl)=>{
    postNative('PresentationImageSave',{scope:mediaTarget.scope,id:mediaTarget.id,dataUrl});
    showToast('Processando e salvando imagem…');
  });
});

document.querySelectorAll('.navbtn').forEach(b=>b.onclick=()=>go(b.dataset.screen));
el('settingsButton').onclick=()=>{el('settingsOverlay').classList.remove('hidden');postNative('GeminiSettingsGet',{});postNative('TtsSettingsGet',{});updateSettingsStatus();updateTtsSettings()};el('settingsClose').onclick=()=>el('settingsOverlay').classList.add('hidden');
function updateSettingsStatus(){if(!el('textSceneStatus'))return;el('textSceneStatus').textContent=geminiState.textSceneConfigured?'Chave configurada':'Chave não configurada';if(geminiState.textModel&&!el('textModel').dataset.dirty)el('textModel').value=geminiState.textModel;if(geminiState.imageModel&&!el('imageModel').dataset.dirty)el('imageModel').value=geminiState.imageModel;const ws=viewState?.worldStatus||{};if(geminiState.ttsModel&&el('ttsModel')&&!el('ttsModel').dataset.dirty)el('ttsModel').value=geminiState.ttsModel;if(el('ttsAudioStatus'))el('ttsAudioStatus').textContent=geminiState.ttsAudioConfigured?'Chave configurada':'Não configurada';el('worldStatus').textContent=ws.active?'Ativo — mapa, NPCs persistentes e agenda de eventos carregados.':'Ainda não inicializado.';const lib=viewState?.libraryStatus||{};el('libraryStatus').textContent=lib.embedded?`Regras incorporadas: ${profileLabel()}. Autoridade carregada.`:'Biblioteca não carregada.'}
function updateTtsSettings(){
 const engine=el('ttsEngine'),voice=el('ttsVoiceSelect'),rate=el('ttsRate'),rateValue=el('ttsRateValue'),status=el('ttsLocalStatus');
 const modeSelect=el('ttsModeSelect'),geminiVoice=el('ttsGeminiVoiceSelect');if(!engine||!voice)return;
 if(modeSelect&&ttsState.mode)modeSelect.value=ttsState.mode;
 if(geminiVoice&&geminiState.ttsVoice)geminiVoice.value=geminiState.ttsVoice;
 const selectedEngine=String(ttsState.enginePackage||'');const selectedVoice=String(ttsState.voiceName||'');
 engine.innerHTML=(ttsState.engines||[]).map(e=>`<option value="${esc(e.packageName)}" ${e.packageName===selectedEngine?'selected':''}>${esc(e.label||e.packageName)}${e.preferredKokoro?' · Kokoro':''}</option>`).join('')||`<option value="">Nenhum motor detectado</option>`;
 voice.innerHTML=(ttsState.voices||[]).map(v=>`<option value="${esc(v.name)}" ${v.name===selectedVoice?'selected':''}>${esc(v.name)} · ${esc(v.localeTag||'pt')} ${v.networkRequired?'· rede':'· local'}</option>`).join('')||`<option value="">Nenhuma voz PT detectada</option>`;
 if(rate&&!rate.dataset.dirty)rate.value=String(ttsState.speechRate||1);if(rateValue)rateValue.textContent=Number(rate?.value||ttsState.speechRate||1).toFixed(2).replace('.',',')+'×';
 if(status){
  const m=ttsState.mode||'ANDROID_TTS';
  status.textContent=m==='OFF'?'Narração em voz desligada (OFF)':m==='GEMINI_TTS'?`Gemini TTS · Voz: ${geminiState.ttsVoice||'Kore'} · pronto`:ttsState.ready?`${(ttsState.enginePackage||'Motor Android')} · ${(ttsState.voiceName||'voz automática')} · pronto`:(ttsState.error?`Erro: ${ttsState.error}`:'Inicializando motor de voz…');
 }
}
['textModel','imageModel'].forEach(id=>el(id)?.addEventListener('input',e=>e.currentTarget.dataset.dirty='1'));
el('saveModels')?.addEventListener('click',()=>{const payload={textModel:el('textModel').value.trim(),imageModel:el('imageModel').value.trim(),ttsModel:el('ttsModel')?.value?.trim()||''};if(postNative('GeminiRuntimeSettingsSet',payload)){['textModel','imageModel'].forEach(id=>delete el(id).dataset.dirty);showToast('Modelos salvos.')}});
document.querySelectorAll('[data-key-save]').forEach(b=>b.onclick=()=>{const slot=b.dataset.keySave||'TEXT_SCENE';const input=slot==='TTS_AUDIO'?el('ttsAudioKey'):el('textSceneKey');const key=input?.value?.trim();if(!key)return showToast('Cole uma chave antes de salvar.');if(postNative('GeminiCredentialSet',{slot,key})){input.value='';showToast('Chave salva no armazenamento seguro.')}});
document.querySelectorAll('[data-key-test]').forEach(b=>b.onclick=()=>{const slot=b.dataset.keyTest||'TEXT_SCENE';if(postNative('GeminiCredentialTest',{slot}))showToast('Testando modelo e conexão…')});
document.querySelectorAll('[data-key-delete]').forEach(b=>b.onclick=()=>{const slot=b.dataset.keyDelete||'TEXT_SCENE';postNative('GeminiCredentialDelete',{slot})});
el('ttsRate')?.addEventListener('input',e=>{e.currentTarget.dataset.dirty='1';el('ttsRateValue').textContent=Number(e.currentTarget.value).toFixed(2).replace('.',',')+'×'});
el('ttsEngine')?.addEventListener('change',()=>{postNative('TtsSettingsSet',{enginePackage:el('ttsEngine').value,voiceName:'',speechRate:Number(el('ttsRate')?.value||1)});showToast('Trocando motor de voz…')});
el('saveTts')?.addEventListener('click',()=>{
 const mode=el('ttsModeSelect')?.value||'ANDROID_TTS',gVoice=el('ttsGeminiVoiceSelect')?.value||'Kore';
 const payload={enginePackage:el('ttsEngine')?.value||'',voiceName:el('ttsVoiceSelect')?.value||'',speechRate:Number(el('ttsRate')?.value||1),mode};
 if(postNative('TtsSettingsSet',payload)){
  delete el('ttsRate').dataset.dirty;
  if(gVoice||el('ttsModel')?.value)postNative('GeminiRuntimeSettingsSet',{ttsVoice:gVoice,ttsModel:el('ttsModel')?.value?.trim()});
  showToast('Motor e voz salvos.');
 }
});
el('testTts')?.addEventListener('click',()=>{if(postNative('TtsTest',{}))showToast('Testando a voz selecionada…')});
el('refreshTts')?.addEventListener('click',()=>postNative('TtsSettingsGet',{}));
el('resetAdventure')?.addEventListener('click',()=>{const ok=confirm('RECOMEÇAR A AVENTURA?\n\nO mundo, tempo, mapa revelado e progresso da campanha serão reiniciados. Sua ficha, inventário, equipamento e configurações Gemini serão preservados.');if(!ok)return;if(postNative('ResetAdventure',{})){el('settingsOverlay').classList.add('hidden');showToast('Recomeçando a aventura com o mesmo personagem…')}});
el('backupExport')?.addEventListener('click',()=>postNative('BackupExport',{}));el('backupImport')?.addEventListener('click',()=>{const backup=el('backupText').value.trim();if(!backup)return showToast('Cole um backup antes de restaurar.');postNative('BackupImport',{backup})});el('backupCopy')?.addEventListener('click',async()=>{const text=el('backupText').value||backupBuffer;if(!text)return showToast('Gere um backup primeiro.');try{await navigator.clipboard.writeText(text);showToast('Backup copiado.')}catch(_e){el('backupText').focus();el('backupText').select();document.execCommand('copy');showToast('Backup copiado.')}});
render();document.body.dataset.ready='true';postNative('ViewState',{request:'initial'});

let guideShowAllTopics = false;
function renderGuideOverlay(forceAll = null) {
  if (forceAll !== null) guideShowAllTopics = forceAll;
  const guide = viewState?.playerGuide || {};
  const contextual = guide.contextualTopics || [];
  const all = guide.topics || [];
  const topics = guideShowAllTopics ? all : (contextual.length ? contextual : all);
  const container = el('guideContent');
  if (!container) return;
  const toggleBtn = `<div style="margin-bottom:10px;text-align:right;"><button class="small" id="guideToggleAllBtn">${guideShowAllTopics ? "Mostrar apenas tópicos contextuais" : "Ver todos os assuntos (" + all.length + ")"}</button></div>`;
  const cardsHtml = topics.map(t => `
    <div class="guide-card" data-topic-id="${esc(t.topicId)}">
      <h4>${esc(t.title)}</h4>
      <span class="pill" style="margin-bottom:4px">${esc(t.category)}</span>
      <p><b>Resumo:</b> ${esc(t.summary)}</p>
      <p>${esc(t.details)}</p>
      <small>Fonte: ${esc(t.sourceRuleRef)}</small>
    </div>
  `).join('') || '<p class="note">Nenhum tópico disponível no momento.</p>';
  container.innerHTML = toggleBtn + cardsHtml;
  el('guideToggleAllBtn')?.addEventListener('click', () => {
    guideShowAllTopics = !guideShowAllTopics;
    renderGuideOverlay();
  });
}

el('guideButton')?.addEventListener('click', () => {
  renderGuideOverlay();
  el('guideOverlay').classList.remove('hidden');
});
el('guideClose')?.addEventListener('click', () => el('guideOverlay').classList.add('hidden'));
el('guideModeLearn')?.addEventListener('click', () => postNative('PlayerGuideModeSet', { mode: 'LEARN_PLAYING' }));
el('guideModeExperienced')?.addEventListener('click', () => postNative('PlayerGuideModeSet', { mode: 'EXPERIENCED' }));

function renderSuggestionsOverlay() {
  const guide = viewState?.playerGuide || {};
  const suggestions = guide.actionSuggestions || [];
  const container = el('suggestionsContent');
  if (!container) return;
  container.innerHTML = suggestions.map(s => {
    const isExec = s.executable !== false;
    const disabledAttr = !isExec ? 'style="opacity: 0.6; cursor: not-allowed; border-color: #664444;"' : '';
    const missingBadge = !isExec && s.missingRequirement ? `<div style="color:#d9534f;font-size:0.8rem;margin-top:4px;"><b>Indisponível:</b> ${esc(s.missingRequirement)}</div>` : '';
    return `
    <div class="suggestion-card ${!isExec ? "disabled" : ""}" ${disabledAttr} data-pick-suggestion="${isExec ? esc(s.textCandidate) : ""}" data-suggestion-intent="${isExec ? esc(s.intentCandidate || "") : ""}">
      <b>${esc(s.plainLabel)}</b>
      <span>${esc(s.textCandidate)}</span>
      <small>Tempo: ${esc(s.timePreview)} · Risco: ${esc(s.riskHint)}</small>
      <small style="color:#8b6537">Regra: ${esc(s.sourceRuleRef)}</small>
      ${missingBadge}
    </div>
  `;
  }).join('') || '<p class="note">Nenhuma sugestão contextual disponível.</p>';
  container.querySelectorAll('[data-pick-suggestion]').forEach(card => {
    if (card.classList.contains('disabled')) return;
    card.onclick = () => {
      const text = card.dataset.pickSuggestion;
      const intent = card.dataset.suggestionIntent;
      if (!text) return;
      const t = el('reactionInput');
      if (t) {
        t.value = text;
        t.dataset.mechanicalIntent = intent;
        t.focus();
      }
      el('suggestionsOverlay').classList.add('hidden');
      showToast('Sugestão aplicada no campo de ação.');
    };
  });
}

if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') document.addEventListener('click', e => { if (e.target && e.target.id === 'openSuggestionsBtn') {
    renderSuggestionsOverlay();
    el('suggestionsOverlay').classList.remove('hidden');
  }
});
el('suggestionsClose')?.addEventListener('click', () => el('suggestionsOverlay').classList.add('hidden'));


// --- GESTÃO DE FOTOS & RETRATOS UNIFICADA ---
let currentPhotoTarget = null;
function openPhotoMenu(scope, id, name) {
  currentPhotoTarget = { scope, id, name };
  const title = el('photoMenuTitle');
  if (title) title.textContent = name ? `Imagem · ${name}` : 'Imagem / Retrato';
  el('photoMenuOverlay')?.classList.remove('hidden');
}

el('photoMenuClose')?.addEventListener('click', () => el('photoMenuOverlay')?.classList.add('hidden'));
el('photoMenuCancel')?.addEventListener('click', () => el('photoMenuOverlay')?.classList.add('hidden'));

el('photoMenuGemini')?.addEventListener('click', () => {
  if (!currentPhotoTarget) return;
  const { scope, id } = currentPhotoTarget;
  if(!geminiState.textSceneConfigured){
    showToast('A geração integrada requer uma chave Gemini com cota de imagens habilitada. A cota gratuita de imagem não está disponível: use a galeria ou a opção externa.');
    return;
  }
  const sent=scope==='SCENE'?postNative('SceneImageGenerate',{}):
    (scope==='CHARACTER'||scope==='NPC'||scope==='ITEM')?
      postNative('PortraitImageGenerate',{kind:scope,id}):false;
  if(!sent){showToast('A geração integrada exige o aplicativo Android e um tipo de imagem válido.');return;}
  el('photoMenuOverlay')?.classList.add('hidden');
  showToast(scope==='SCENE'?'Solicitando a imagem de cena ao Gemini…':'Solicitando a imagem ao Gemini…');
});

el('photoMenuGeminiApp')?.addEventListener('click', () => {
  if (!currentPhotoTarget) return;
  const { scope, id } = currentPhotoTarget;
  el('photoMenuOverlay')?.classList.add('hidden');
  const action = scope === 'SCENE' ? 'ScenePromptShare' : 'PortraitPromptShare';
  const payload = scope === 'SCENE' ? {} : { kind: scope, id };
  if (postNative(action, payload)) {
    showToast('Abrindo o Gemini externo. Depois de gerar, compartilhe a imagem com o Braseiro OSE ou salve e importe pela galeria.');
  } else {
    showToast('O Gemini externo requer o aplicativo Android.');
  }
});

el('photoMenuGallery')?.addEventListener('click', () => {
  if (!currentPhotoTarget) return;
  const { scope, id } = currentPhotoTarget;
  el('photoMenuOverlay')?.classList.add('hidden');
  chooseMedia(scope, id);
});

el('photoMenuRemove')?.addEventListener('click', () => {
  if (!currentPhotoTarget) return;
  const { scope, id } = currentPhotoTarget;
  el('photoMenuOverlay')?.classList.add('hidden');
  postNative('PresentationImageRemove', { scope, id });
  showToast('Removendo imagem…');
});

// --- PRIMEIRO LANÇAMENTO / ESCOLHA DE MODO ---
let firstLaunchDismissed = false;
function checkFirstLaunch() {
  if (viewState?.playerGuide?.firstLaunch && !firstLaunchDismissed) {
    el('firstLaunchOverlay')?.classList.remove('hidden');
  }
}

el('firstLaunchLearn')?.addEventListener('click', () => {
  firstLaunchDismissed = true;
  postNative('PlayerGuideModeSet', { mode: 'LEARN_PLAYING' });
  el('firstLaunchOverlay')?.classList.add('hidden');
  showToast('Modo Aprender Jogando ativado!');
});

el('firstLaunchExperienced')?.addEventListener('click', () => {
  firstLaunchDismissed = true;
  postNative('PlayerGuideModeSet', { mode: 'EXPERIENCED' });
  el('firstLaunchOverlay')?.classList.add('hidden');
  showToast('Modo Jogador Experiente ativado!');
});

// --- TÓPICOS DO GUIA POR CLIQUE CONTEXTUAL ---
function openGuideTopic(topicId) {
  renderGuideOverlay(true);
  el('guideOverlay')?.classList.remove('hidden');
  setTimeout(() => {
    const cards = document.querySelectorAll('.guide-card');
    cards.forEach(c => {
      const match = c.dataset.topicId === topicId;
      if (match) {
        c.scrollIntoView({ behavior: 'smooth', block: 'center' });
        c.style.boxShadow = '0 0 10px #d9a84c';
      }
    });
  }, 50);
}

if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') document.addEventListener('click', e => { const target = e.target && e.target.closest ? e.target.closest('[data-guide-topic]') : null;
  if (target) {
    e.stopPropagation();
    openGuideTopic(target.dataset.guideTopic);
  }
});
})();
