import { NativePersistencePort, MemoryPersistenceAdapter } from './core/persistence-port.js';
import { ensureStarterCampaign } from './core/starter-bootstrap.js';
import { SceneSessionService } from './core/scene-session-service.js';
import { createRuntimeNarrativeProvider } from './core/runtime-narrative-provider.js';
import { createSceneImageClient } from './core/scene-image-client.js';
import { buildSceneVisualPacket, buildSceneGenerationPrompt, sceneReferenceImages } from './core/scene-visual-packet.js';
import { createGeminiImageClient } from './core/gemini-image-client.js';
import { canonicalJson } from './core/canonical-json.js';
import { characterStatusProjection } from './core/party-state.js';
import { resolveActiveCampaignId, setRuntimeActiveCampaignId } from './core/runtime-campaign.js';
import { createAudioSessionCoordinator } from './core/audio-session.js';
import { createNativeTtsProvider } from './core/native-tts-provider.js';
import { TtsPresentationService } from './core/tts-presentation.js';
import { PlayerDeclarationService } from './core/player-declaration-service.js';

const pack=await fetch('data/wwn.starter-region.v1.json').then(r=>r.json());
const canonicalIria=await fetch('data/wave6.iria.canonical.fixture.json').then(r=>r.json());
const persistence=globalThis.BraseiroNative?new NativePersistencePort():new MemoryPersistenceAdapter();
const boot=ensureStarterCampaign({persistence,character:canonicalIria,pack});
if(!String(boot.status).startsWith('OK'))throw new Error(`Starter boot failed: ${boot.status}`);
const campaignId=resolveActiveCampaignId(persistence)??boot.campaignId;setRuntimeActiveCampaignId(campaignId);
const barbara=createRuntimeNarrativeProvider();
const scenes=new SceneSessionService({persistence,barbara});
const sceneImages=createSceneImageClient();
const geminiImages=createGeminiImageClient();
const audio=createAudioSessionCoordinator();
const tts=new TtsPresentationService({provider:createNativeTtsProvider()});
const declarations=new PlayerDeclarationService({persistence,barbara});
let loaded=scenes.load({campaignId});let projection=loaded.projection;

const $=id=>document.getElementById(id);const content=projection.content;
function renderMasterStatus(){
  const status=barbara.status?.()??{configured:false};
  const el=$('masterProviderStatus');if(!el)return;
  if(status.configured===true){el.textContent=`MESTRE: GEMINI • ${status.model??'modelo ativo'}`;el.className='ok';}
  else{el.textContent='MESTRE: LOCAL • configure Gemini em AJUSTES';el.className='warn';}
}
async function narrateCurrentScene(){
  const button=$('sceneMasterButton');const note=$('sceneImageStatus');
  const scene=projection.scene;if(!scene||scene.status!=='OK'){note.textContent='Abra um gancho ou local antes de pedir a narração do Mestre.';return;}
  const status=barbara.status?.()??{configured:false};
  if(status.configured!==true){renderMasterStatus();note.textContent='Gemini ainda não está configurado. Toque em AJUSTES, salve a chave API e volte para narrar com Barbara.';return;}
  button.disabled=true;button.textContent='MESTRE NARRANDO…';note.textContent='Barbara está narrando a cena atual…';
  try{
    const loadedState=persistence.loadCampaign(campaignId);if(loadedState.status!=='OK')throw new Error('Campanha ativa indisponível.');
    const state=JSON.parse(loadedState.stateJson);const parentId=state.journalState?.lastSceneEventId;
    const stored=parentId&&typeof persistence.getEvent==='function'?persistence.getEvent(parentId)?.event:null;
    let mechanicalEvent=null;
    if(stored?.payloadJson){try{mechanicalEvent=JSON.parse(stored.payloadJson)}catch{}}
    mechanicalEvent=mechanicalEvent??{eventId:parentId??`scene:${campaignId}:r${loadedState.revision}:current`,revision:stored?.revision??loadedState.revision,kind:'SCENE_OPENED',contentType:scene.contentType,contentId:scene.contentId,scene,mechanicalMutation:false,timeAdvanced:false,positionChanged:false};
    if(!mechanicalEvent.scene)mechanicalEvent={...mechanicalEvent,scene};
    const narrative=barbara.generateAsync?await barbara.generateAsync({mechanicalEvent,state}):await barbara.generate({mechanicalEvent,state});
    if(!String(narrative?.text??'').trim())throw new Error(narrative?.reason??narrative?.status??'Narração vazia.');
    if(parentId&&stored){
      const eventId=`narrative:${parentId}:manual:${Date.now()}`;
      persistence.appendNarrative({campaignId,eventId,mechanicalEventId:parentId,mechanicalRevision:stored.revision,eventPayloadJson:canonicalJson(narrative)});
    }
    loaded={...loaded,narrative};renderScene(narrative);
    const hasSceneImage=Boolean(projection.scene?.imageRef??projection.scene?.imageUrl);const imageNote=narrative.imageAttached===true?' • imagem da cena analisada':(hasSceneImage&&status.visionEnabled!==false?' • ATENÇÃO: a imagem não chegou ao Gemini':'');
    note.textContent=`Narração atualizada por ${narrative.provider==='GEMINI_NATIVE'?'Gemini':'Barbara local'}${imageNote}.`;
    const voiceSettings=audio.status();
    if(voiceSettings.masterVoiceEnabled!==false){
      const spoken=await tts.speakVisible({text:String(narrative.text).trim(),visibility:'PLAYER_VISIBLE',rate:.96,pitch:.96,role:'NARRADOR'});
      if(spoken.status!=='OK')note.textContent+=` Voz: ${spoken.reason??spoken.status}.`;
    }
  }catch(error){note.textContent=`Não foi possível narrar agora: ${error?.message??error}`;}
  finally{button.disabled=false;button.textContent='MESTRE NARRAR AGORA';renderMasterStatus();}
}

$('campaignName').textContent=projection.displayName??content?.campaign??'SOMBRAS DE GELO E FERRO';$('positionName').textContent=projection.world?.position?.name??'POSIÇÃO ATUAL';
function activeCharacter(){const loadedState=persistence.loadCampaign(campaignId);if(loadedState.status==='OK'){try{const character=JSON.parse(loadedState.stateJson).characterState;const bars=characterStatusProjection(character);if(character&&bars.status==='OK')return{name:character.name??character.characterId,statusBars:bars}}catch{}}const g=projection.party?.groups?.[projection.party.activeGroupId]??Object.values(projection.party?.groups??{})[0];return g?.members?.[0]??null}
function currentCanonicalState(){const l=persistence.loadCampaign(campaignId);if(l.status!=='OK')return {};try{return JSON.parse(l.stateJson)}catch{return {}}}
function syncAudio(){const st=currentCanonicalState();return audio.sync({state:st,scene:projection.scene,combatActive:st?.encounterState?.combat?.active===true,journey:false})}
function renderBars(){const m=activeCharacter();if(!m)return;const b=m.statusBars??m;$('statusName').textContent=m.name;$('hpFill').style.width=`${Math.round((b?.hp?.fill??0)*100)}%`;$('strainFill').style.width=`${Math.round((b?.systemStrain?.fill??0)*100)}%`;$('hpText').textContent=b?.hp?`${b.hp.current} / ${b.hp.max}`:'—';$('strainText').textContent=b?.systemStrain?`${b.systemStrain.current} / ${b.systemStrain.max}`:'—'}
function card(item,type){const el=document.createElement('button');el.className='card';el.innerHTML=`<span class="kind">${type==='startingHook'?'GANCHO':'LOCAL'}</span><b>${item.title??item.name}</b><span>${item.premise??item.summary??item.situation??''}</span>`;el.onclick=()=>openScene(type,item.id);return el}
for(const h of content?.startingHooks??[])$('hooks').appendChild(card(h,'startingHook'));for(const s of content?.majorSites??[])$('sites').appendChild(card(s,'majorSite'));
function renderScene(narrative=null){const scene=projection.scene;if(!scene||scene.status!=='OK'){$('sceneCard').classList.add('hidden');return}$('sceneCard').classList.remove('hidden');$('sceneTitle').textContent=scene.title;$('sceneNarrative').textContent=narrative?.text??scene.summary;const leads=$('sceneLeads');leads.innerHTML='';for(const x of scene.leads??[]){const l=document.createElement('span');l.className='lead';l.textContent=x;leads.appendChild(l)}const imageRef=projection.scene?.imageRef??projection.scene?.imageUrl??null;const imported=$('sceneImportedImage');if(imageRef){imported.src=imageRef;imported.classList.remove('hidden')}else{imported.removeAttribute('src');imported.classList.add('hidden')}const observe=scene.contentType==='majorSite'&&scene.contentId==='site_vigilia';$('observeButton').classList.toggle('hidden',!observe)}
async function openScene(contentType,contentId){const result=await scenes.openContentScene({campaignId,contentType,contentId});if(!['OK','SCENE_ALREADY_OPEN_NO_MUTATION'].includes(result.status))return;loaded=scenes.load({campaignId});projection=loaded.projection;renderScene(result.narrative??loaded.narrative);syncAudio();document.querySelector('.content').scrollTo({top:0,behavior:'smooth'})}
$('observeButton').onclick=()=>{location.href='scr015.html'};
function currentSceneVisual(){const st=currentCanonicalState();const packet=buildSceneVisualPacket({state:st,scene:projection.scene,narrative:loaded.narrative,showParty:true});return {state:st,packet,prompt:buildSceneGenerationPrompt(packet),references:sceneReferenceImages(packet)}}
function attachSceneImageResult(result,{generated=false,interactionId=null,model=null}={}){const l=persistence.loadCampaign(campaignId);if(l.status!=='OK')return {status:'CAMPAIGN_UNAVAILABLE'};const st=JSON.parse(l.stateJson),next=structuredClone(st),newRevision=l.revision+1;next.revision=newRevision;next.encounterState={...(next.encounterState??{}),currentScene:{...(next.encounterState?.currentScene??{}),imageRef:result.url,imageSha256:result.sha256}};if(generated){const prior=next.aiState?.sceneVisualSession??{};next.aiState={...(next.aiState??{}),sceneVisualSession:{previousInteractionId:interactionId??prior.previousInteractionId??null,model:model??prior.model??null,sequence:(prior.sequence??0)+1,lastSceneRevision:newRevision,lastImageSha256:result.sha256??null}};}const eventId=`scene-image:${campaignId}:r${newRevision}`;const receipt=persistence.commit({campaignId,expectedRevision:l.revision,eventId,eventKind:generated?'SCENE_IMAGE_GENERATED':'SCENE_IMAGE_ATTACHED',eventPayloadJson:canonicalJson({eventId,revision:newRevision,url:result.url,sha256:result.sha256,width:result.width,height:result.height,generated,interactionId:interactionId??null,mechanicalMutation:false}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});if(receipt.status==='OK'){loaded=scenes.load({campaignId});projection=loaded.projection;renderScene(loaded.narrative);}return {status:receipt.status,receipt}}
$('sceneMasterButton').onclick=narrateCurrentScene;
$('sceneGenerateButton').onclick=async()=>{const button=$('sceneGenerateButton'),note=$('sceneImageStatus');const status=geminiImages.status();if(status.status!=='OK'||status.configured!==true){note.textContent='Configure a chave Gemini em AJUSTES antes de gerar a cena.';return;}const {state,packet,prompt,references}=currentSceneVisual();const session=state?.aiState?.sceneVisualSession??{};const previousInteractionId=session.model===status.imageModel?session.previousInteractionId??null:null;button.disabled=true;button.textContent='GERANDO…';note.textContent=`Gerando a cena com ${packet.partyCount} integrante(s) e ${references.length} referência(s) persistente(s)…`;try{const r=await geminiImages.generate({prompt,model:status.imageModel,aspectRatio:'16:9',imageSize:'1K',previousInteractionId,referenceImageUrls:references});if(r.status!=='OK'){note.textContent=`Imagem não gerada: ${r.reason??r.status}`;return;}const attached=attachSceneImageResult(r,{generated:true,interactionId:r.interactionId,model:r.model??status.imageModel});note.textContent=attached.status==='OK'?`Cena gerada e vinculada. Sessão visual contínua: ${(state?.aiState?.sceneVisualSession?.sequence??0)+1}.`:`Imagem gerada, mas não vinculada: ${attached.status}`;}catch(error){note.textContent=`Falha ao gerar cena: ${error?.message??error}`;}finally{button.disabled=false;button.textContent='GERAR IMAGEM DA CENA'}};
$('scenePromptButton').onclick=()=>{const {prompt}=currentSceneVisual();if(globalThis.BraseiroNative?.shareText){const r=JSON.parse(globalThis.BraseiroNative.shareText('Prompt da cena WWN',prompt));$('sceneImageStatus').textContent=r.status==='OK'?'Prompt visual compartilhado.':`Não foi possível compartilhar: ${r.reason??r.status}`;}else if(navigator.clipboard?.writeText){navigator.clipboard.writeText(prompt);$('sceneImageStatus').textContent='Prompt visual copiado.';}else{$('sceneImageStatus').textContent=prompt;}};
$('sceneImageButton').onclick=()=>{const r=sceneImages.requestImport();$('sceneImageStatus').textContent=r.status==='PENDING'?'Escolha uma imagem local para esta cena.':`Importação indisponível: ${r.reason??r.status}`};
$('sceneSpeakButton').onclick=async()=>{const settings=audio.status();if(settings.masterVoiceEnabled===false){$('sceneImageStatus').textContent='Narração do Mestre está desativada em Configurações.';return;}const visible=$('sceneNarrative').textContent.trim();if(!visible){$('sceneImageStatus').textContent='Não há narração visível para ler.';return;}const r=await tts.speakVisible({text:visible,visibility:'PLAYER_VISIBLE',rate:.96,pitch:.96,role:'NARRADOR'});$('sceneImageStatus').textContent=r.status==='OK'?`Narração PT-BR iniciada em ${r.unitCount} trecho(s).`:`Voz indisponível: ${r.reason??r.status}`;};
$('sceneStopVoiceButton').onclick=()=>{tts.stop();$('sceneImageStatus').textContent='Narração interrompida.';};
$('playerReactionButton').onclick=async()=>{const button=$('playerReactionButton'),input=$('playerReactionInput'),status=$('playerReactionStatus');const text=input.value.trim();if(!text){status.textContent='Escreva o que seu personagem faz ou diz.';return;}if((barbara.status?.()??{}).configured!==true){status.textContent='Configure a chave e o modelo Gemini em AJUSTES para usar ações livres.';return;}button.disabled=true;button.textContent='MESTRE RESPONDENDO…';status.textContent='Barbara está interpretando sua declaração sem alterar a mecânica.';try{const result=await declarations.declare({campaignId,text});if(result.status!=='OK'){status.textContent=result.status==='DECLARATION_COMMITTED_NARRATIVE_PENDING'?'Sua declaração foi registrada, mas a resposta do Mestre falhou. Tente MESTRE NARRAR AGORA.':`Ação não concluída: ${result.status}`;return;}loaded=scenes.load({campaignId});projection=loaded.projection;renderScene(result.narrative??loaded.narrative);input.value='';status.textContent=result.narrative?.imageAttached===true?'Mestre respondeu usando também a imagem da cena.':'Mestre respondeu. Nenhuma mecânica foi alterada pela declaração.';if(audio.status().masterVoiceEnabled!==false&&result.narrative?.text)void tts.speakVisible({text:result.narrative.text,visibility:'PLAYER_VISIBLE',rate:.96,pitch:.96,role:'NARRADOR'});}catch(error){status.textContent=`Não foi possível responder agora: ${error?.message??error}`;}finally{button.disabled=false;button.textContent='ENVIAR AO MESTRE';}};
sceneImages.onImport(result=>{if(result.status!=='OK'){$('sceneImageStatus').textContent=result.status==='CANCELLED'?'Importação cancelada.':`Imagem não importada: ${result.reason??result.status}`;return;}const attached=attachSceneImageResult(result,{generated:false});$('sceneImageStatus').textContent=attached.status==='OK'?'Imagem importada e vinculada à cena atual.':`Imagem importada, mas não vinculada: ${attached.status}`;});
$('statusButton').onclick=()=>{$('shade').classList.remove('hidden');$('statusPopup').classList.remove('hidden')};$('closeStatus').onclick=$('shade').onclick=()=>{$('shade').classList.add('hidden');$('statusPopup').classList.add('hidden')};
renderBars();renderScene(loaded.narrative);renderMasterStatus();syncAudio();window.__BRASEIRO_JORNADA__={campaignId,projection:()=>structuredClone(projection),openScene};
