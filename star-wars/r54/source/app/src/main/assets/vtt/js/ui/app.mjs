import { LocalStorageRepository } from '../core/storage.mjs';
import { GameController } from '../core/controller.mjs';
import { SKILLS } from '../rules/character.mjs';
import { getHealthStatus } from '../rules/health.mjs';
import { CHARACTERISTIC_LABELS_PT_BR, SKILL_LABELS_PT_BR } from '../rules/labelsPtBr.mjs';
import { availableTargetRanges, attackDifficultyForWeapon } from '../rules/attackDifficulty.mjs';
import { currentSlot } from '../rules/encounter.mjs';
import { adversaryHealthStatus, aliveMinions, upgradeDifficultyForAdversaryTalent } from '../rules/adversaries.mjs';
import { nativeLibraryAvailable, listLibraryDocuments, searchImportedLibrary, requestPdfImport, removeLibraryDocument, reindexLibraryDocument, updateLibraryDocumentPolicy, libraryStatusLabel, libraryAuthorityTypeLabel } from '../library/nativeLibrary.mjs';
import { skillRankCostForCharacter, isCareerSkill, MAX_SKILL_RANK } from '../rules/progression.mjs';
import { SESSION_PHASE_LABELS_PT_BR } from '../master/sessionFlow.mjs';
import { EOTE_SPECIES, EOTE_CAREERS, OBLIGATION_TYPES_PT_BR, eoteCreationPreview } from '../rules/eoteCharacterCreation.mjs';
import { vehicleStatus, PLANETARY_RANGE_BANDS } from '../rules/vehicles.mjs';
import { speakNarration, stopNarration } from './tts.mjs';
import { IMAGE_LIBRARY_MANIFEST } from '../library/imageLibraryData.mjs';
import { getImageAsset, searchImageAssets, imageAssetRuntimeDescriptor } from '../library/imageLibrary.mjs';

const repo = new LocalStorageRepository();
const game = new GameController(repo);
const $ = id => document.getElementById(id);
const escapeHtml = value => String(value ?? '').replace(/[&<>\"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]));

const galaxyMapUi={scale:1,x:0,y:0,dragging:false,pointerId:null,lastX:0,lastY:0};
function clampMapScale(value){ return Math.max(1,Math.min(4,Number(value)||1)); }
function applyGalaxyMapTransform(){
  const img=$('galaxyMapImage'); if(!img) return;
  img.style.transform=`translate(${galaxyMapUi.x}px, ${galaxyMapUi.y}px) scale(${galaxyMapUi.scale})`;
}
function resetGalaxyMap(){ galaxyMapUi.scale=1; galaxyMapUi.x=0; galaxyMapUi.y=0; applyGalaxyMapTransform(); }
function zoomGalaxyMap(delta){
  galaxyMapUi.scale=clampMapScale(galaxyMapUi.scale+delta);
  if(galaxyMapUi.scale===1){ galaxyMapUi.x=0; galaxyMapUi.y=0; }
  applyGalaxyMapTransform();
}

const mapLibraryUi={selectedAssetId:'local.map.galaxy',statuses:new Map(),query:''};
function nativeImageLibrary(){ return globalThis.BraseiroImages && typeof globalThis.BraseiroImages.getStatus==='function' ? globalThis.BraseiroImages : null; }
function parseNativeJson(raw){ try{ const value=typeof raw==='string'?JSON.parse(raw):raw; return value&&typeof value==='object'?value:null; }catch{return null;} }
function localImageUrl(asset){ return asset?.localPath ? asset.localPath : null; }
function mapAssets(query=''){
  return searchImageAssets(IMAGE_LIBRARY_MANIFEST,{category:'map',query,limit:200});
}
function getMapCacheStatus(assetId){
  if(mapLibraryUi.statuses.has(assetId)) return mapLibraryUi.statuses.get(assetId);
  const bridge=nativeImageLibrary(); if(!bridge) return null;
  const parsed=parseNativeJson(bridge.getStatus(assetId));
  if(parsed?.ok) mapLibraryUi.statuses.set(assetId,parsed);
  return parsed?.ok?parsed:null;
}
function mapImageUrl(asset){
  if(!asset) return null;
  if(asset.sourceType!=='external_reference') return localImageUrl(asset);
  const cached=getMapCacheStatus(asset.assetId);
  if(cached?.cached && cached.runtimeUrl) return cached.runtimeUrl;
  // Browser preview fallback. Android uses only private validated cache for remote assets.
  return nativeImageLibrary()?null:(asset.remoteReady?asset.remoteUrl:null);
}
function renderMapLibrary(){
  const list=$('imageMapCatalog'); if(!list) return;
  const query=$('imageMapSearch')?.value?.trim()||''; mapLibraryUi.query=query;
  const assets=mapAssets(query);
  if(!assets.some(a=>a.assetId===mapLibraryUi.selectedAssetId)) mapLibraryUi.selectedAssetId=assets[0]?.assetId||null;
  list.innerHTML=assets.length?assets.map(asset=>{
    const cached=asset.sourceType==='external_reference'?getMapCacheStatus(asset.assetId):null;
    const offline=asset.sourceType!=='external_reference'||cached?.cached;
    return `<button type="button" class="map-library-item${asset.assetId===mapLibraryUi.selectedAssetId?' active':''}" data-map-asset="${escapeHtml(asset.assetId)}"><strong>${escapeHtml(asset.title)}</strong><span>${escapeHtml(asset.mapKind||asset.subtype||'mapa')} · ${offline?'offline':'online'}</span></button>`;
  }).join(''):'<p class="microcopy">Nenhum mapa encontrado.</p>';
  renderSelectedMapAsset();
}
function renderSelectedMapAsset(){
  const asset=getImageAsset(IMAGE_LIBRARY_MANIFEST,mapLibraryUi.selectedAssetId);
  const title=$('imageMapSelectedTitle'), meta=$('imageMapSelectedMeta'), source=$('imageMapSource'), cache=$('imageMapCache'), clear=$('imageMapClearCache');
  if(!asset){ if(title) title.textContent='Nenhum mapa selecionado'; return; }
  const status=asset.sourceType==='external_reference'?getMapCacheStatus(asset.assetId):null;
  const imgUrl=mapImageUrl(asset); const img=$('galaxyMapImage');
  if(imgUrl){ img.src=imgUrl; img.alt=asset.title; resetGalaxyMap(); }
  if(title) title.textContent=asset.title;
  if(meta) meta.textContent=`${asset.mapKind||asset.subtype||'mapa'} · ${asset.sourceType==='external_reference'?(status?.cached?'cache offline':'fonte externa'):'empacotado'}`;
  if(source){ source.hidden=!asset.sourcePageUrl; source.href=asset.sourcePageUrl||'#'; }
  if(cache){ cache.hidden=asset.sourceType!=='external_reference'||Boolean(status?.cached); cache.disabled=!asset.remoteReady||!nativeImageLibrary(); }
  if(clear){ clear.hidden=asset.sourceType!=='external_reference'||!status?.cached; }
  const feedback=$('imageMapLibraryFeedback');
  if(feedback && asset.sourceType==='external_reference' && !status?.cached && nativeImageLibrary()) feedback.textContent='Mapa externo indexado. Toque em “Guardar offline” para baixar pela origem permitida.';
  else if(feedback && imgUrl) feedback.textContent='Seleção visual apenas: não altera posição, viagem, tempo ou estado da campanha.';
}
function cacheSelectedMap(){
  const asset=getImageAsset(IMAGE_LIBRARY_MANIFEST,mapLibraryUi.selectedAssetId); const bridge=nativeImageLibrary();
  if(!asset||asset.sourceType!=='external_reference'||!bridge) return;
  const descriptor=imageAssetRuntimeDescriptor(IMAGE_LIBRARY_MANIFEST,asset);
  if(!descriptor?.remoteReady) return;
  const r=parseNativeJson(bridge.cacheRemoteImage(asset.assetId,descriptor.remoteUrl));
  const feedback=$('imageMapLibraryFeedback'); if(feedback) feedback.textContent=r?.ok?'Download enfileirado. O mapa aparecerá offline quando o cache terminar.':(r?.message||'Não foi possível iniciar o cache.');
}
function clearSelectedMapCache(){
  const asset=getImageAsset(IMAGE_LIBRARY_MANIFEST,mapLibraryUi.selectedAssetId); const bridge=nativeImageLibrary(); if(!asset||!bridge) return;
  const r=parseNativeJson(bridge.clearCachedAsset(asset.assetId)); if(r?.ok) mapLibraryUi.statuses.set(asset.assetId,{ok:true,assetId:asset.assetId,cached:false});
  renderMapLibrary();
}

const visualLibraryUi={selectedAssetId:null,query:'',kind:'all'};
function visualEntityAssets(query='',kind='all'){
  const common={query,sourceType:'external_reference',limit:500};
  let items=[];
  if(kind==='all'||kind==='npc') items.push(...searchImageAssets(IMAGE_LIBRARY_MANIFEST,{...common,category:'portrait',subtype:'npc'}));
  if(kind==='all'||kind==='droid') items.push(...searchImageAssets(IMAGE_LIBRARY_MANIFEST,{...common,category:'droid'}));
  if(kind==='all'||kind==='beast') items.push(...searchImageAssets(IMAGE_LIBRARY_MANIFEST,{...common,category:'creature',subtype:'beast'}));
  if(kind==='all'||kind==='vehicle') items.push(...searchImageAssets(IMAGE_LIBRARY_MANIFEST,{...common,category:'vehicle'}));
  return items.sort((a,b)=>a.title.localeCompare(b.title,'pt-BR')||a.assetId.localeCompare(b.assetId));
}
function visualEntityImageUrl(asset){
  if(!asset) return null;
  const cached=getMapCacheStatus(asset.assetId);
  if(cached?.cached&&cached.runtimeUrl) return cached.runtimeUrl;
  return nativeImageLibrary()?null:(asset.remoteReady?asset.remoteUrl:null);
}
function renderVisualLibrary(){
  const list=$('imageVisualCatalog'); if(!list) return;
  visualLibraryUi.query=$('imageVisualSearch')?.value?.trim()||'';
  visualLibraryUi.kind=$('imageVisualKind')?.value||'all';
  const assets=visualEntityAssets(visualLibraryUi.query,visualLibraryUi.kind);
  if(!assets.some(a=>a.assetId===visualLibraryUi.selectedAssetId)) visualLibraryUi.selectedAssetId=assets[0]?.assetId||null;
  list.innerHTML=assets.length?assets.slice(0,60).map(asset=>{
    const cached=getMapCacheStatus(asset.assetId); const kind=asset.category==='portrait'?'NPC':asset.category==='droid'?'droide':asset.category==='vehicle'?'veículo/nave':'criatura';
    return `<button type="button" class="map-library-item${asset.assetId===visualLibraryUi.selectedAssetId?' active':''}" data-visual-asset="${escapeHtml(asset.assetId)}"><strong>${escapeHtml(asset.title)}</strong><span>${kind} · ${cached?.cached?'offline':'online'}</span></button>`;
  }).join(''):'<p class="microcopy">Nenhuma referência visual encontrada.</p>';
  renderSelectedVisualAsset();
}
function renderSelectedVisualAsset(){
  const asset=getImageAsset(IMAGE_LIBRARY_MANIFEST,visualLibraryUi.selectedAssetId);
  const title=$('imageVisualSelectedTitle'),meta=$('imageVisualSelectedMeta'),source=$('imageVisualSource'),cache=$('imageVisualCache'),clear=$('imageVisualClearCache'),preview=$('imageVisualPreview');
  if(!asset){ if(title) title.textContent='Nenhuma referência selecionada'; if(preview) preview.hidden=true; return; }
  const status=getMapCacheStatus(asset.assetId); const url=visualEntityImageUrl(asset); const kind=asset.category==='portrait'?'NPC':asset.category==='droid'?'droide':asset.category==='vehicle'?'veículo/nave':'criatura';
  if(title) title.textContent=asset.title;
  if(meta) meta.textContent=`${kind} · ${status?.cached?'cache offline':'fonte externa'} · sem vínculo mecânico`;
  if(source){source.hidden=!asset.sourcePageUrl;source.href=asset.sourcePageUrl||'#';}
  if(cache){cache.hidden=Boolean(status?.cached);cache.disabled=!asset.remoteReady||!nativeImageLibrary();}
  if(clear){clear.hidden=!status?.cached;}
  if(preview){ if(url){preview.src=url;preview.alt=asset.title;preview.hidden=false;} else {preview.removeAttribute('src');preview.hidden=true;} }
}
function cacheSelectedVisualAsset(){
  const asset=getImageAsset(IMAGE_LIBRARY_MANIFEST,visualLibraryUi.selectedAssetId), bridge=nativeImageLibrary(); if(!asset||!bridge) return;
  const descriptor=imageAssetRuntimeDescriptor(IMAGE_LIBRARY_MANIFEST,asset); if(!descriptor?.remoteReady) return;
  const r=parseNativeJson(bridge.cacheRemoteImage(asset.assetId,descriptor.remoteUrl)); const feedback=$('imageVisualFeedback');
  if(feedback) feedback.textContent=r?.ok?'Download enfileirado. A referência ficará offline quando o cache terminar.':(r?.message||'Não foi possível iniciar o cache.');
}
function clearSelectedVisualAssetCache(){
  const asset=getImageAsset(IMAGE_LIBRARY_MANIFEST,visualLibraryUi.selectedAssetId), bridge=nativeImageLibrary(); if(!asset||!bridge) return;
  const r=parseNativeJson(bridge.clearCachedAsset(asset.assetId)); if(r?.ok) mapLibraryUi.statuses.set(asset.assetId,{ok:true,assetId:asset.assetId,cached:false}); renderVisualLibrary();
}
globalThis.braseiroImageLibraryNativeEvent=raw=>{
  const event=parseNativeJson(raw); const assetId=event?.data?.assetId; if(assetId){ mapLibraryUi.statuses.delete(assetId); if(assetId===mapLibraryUi.selectedAssetId) renderMapLibrary(); if(assetId===visualLibraryUi.selectedAssetId) renderVisualLibrary(); }
};


function checkedValues(hostId) {
  return [...$(hostId).querySelectorAll('input[type="checkbox"]:checked')].map(input=>input.value);
}

function populateSkillChecks(hostId, skills, namePrefix) {
  const host=$(hostId); host.innerHTML='';
  for (const key of skills) {
    const label=document.createElement('label'); label.className='creator-skill-option';
    label.innerHTML=`<input type="checkbox" value="${escapeHtml(key)}" name="${escapeHtml(namePrefix)}"> <span>${escapeHtml(SKILL_LABELS_PT_BR[key] ?? key)}</span>`;
    host.append(label);
  }
}

function creatorSelections() {
  const speciesKey=$('customSpecies').value;
  const careerKey=$('customCareer').value;
  const specializationKey=$('customSpecialization').value;
  const characteristics={};
  for (const input of $('customCharacteristics').querySelectorAll('input[data-characteristic]')) characteristics[input.dataset.characteristic]=Number(input.value);
  return {
    name:$('customName').value,
    speciesKey,careerKey,specializationKey,
    partySize:1,startingObligation:Number($('customObligation').value),obligationType:$('customObligationType').value,
    obligationBenefits:{xp5:$('obligationXp5').checked,xp10:$('obligationXp10').checked,credits1000:$('obligationCredits').checked,credits2500:$('obligationCredits2500').checked},
    speciesVariant:speciesKey==='gand'?$('customSpeciesVariant').value:null,
    characteristics,
    careerFreeSkills:checkedValues('careerFreeSkills'),
    specializationFreeSkills:checkedValues('specializationFreeSkills'),
    speciesChoiceSkills:checkedValues('speciesChoiceSkills'),
    motivation:$('customMotivation').value,
    background:$('customBackground').value
  };
}

function renderCreatorCharacteristics(speciesKey, preserve={}) {
  const species=EOTE_SPECIES[speciesKey]; const host=$('customCharacteristics'); host.innerHTML='';
  if (!species) return;
  for (const [key,base] of Object.entries(species.characteristics)) {
    const box=document.createElement('label'); box.className='creator-stat';
    const val=Math.max(base,Math.min(5,Number(preserve[key] ?? base)));
    box.innerHTML=`<span>${escapeHtml(CHARACTERISTIC_LABELS_PT_BR[key] ?? key)}</span><input data-characteristic="${escapeHtml(key)}" type="number" min="${base}" max="5" value="${val}"><small>base ${base}</small>`;
    host.append(box);
  }
}

function renderSpeciesChoice(speciesKey, careerKey, specializationKey) {
  const species=EOTE_SPECIES[speciesKey]; const wrap=$('speciesChoiceWrap'); const host=$('speciesChoiceSkills'); host.innerHTML='';
  const variantWrap=$('speciesVariantWrap');
  if(variantWrap) variantWrap.hidden=species?.speciesChoice?.type!=='gandRespiration';
  if (!species?.speciesChoice || species.speciesChoice.type==='gandRespiration') { wrap.hidden=true; return; }
  wrap.hidden=false;
  if (species.speciesChoice.type==='oneSkill') {
    $('speciesChoiceHint').textContent='Espécie: escolha 1 das perícias abaixo para receber 1 grau gratuito.';
    populateSkillChecks('speciesChoiceSkills',species.speciesChoice.skills,'speciesChoice');
    return;
  }
  const career=EOTE_CAREERS[careerKey]; const spec=career?.specializations?.[specializationKey];
  const careerSet=new Set([...(career?.skills ?? []),...(spec?.skills ?? [])]);
  const nonCareer=Object.keys(SKILLS).filter(key=>!careerSet.has(key));
  $('speciesChoiceHint').textContent='Humano: escolha exatamente 2 perícias não profissionais diferentes.';
  populateSkillChecks('speciesChoiceSkills',nonCareer,'speciesChoice');
}

function updateCharacterCreator({resetCharacteristics=false}={}) {
  const speciesKey=$('customSpecies').value;
  const careerKey=$('customCareer').value;
  const career=EOTE_CAREERS[careerKey];
  const specSelect=$('customSpecialization'); const previousSpec=specSelect.value;
  specSelect.innerHTML='';
  for (const [key,spec] of Object.entries(career?.specializations ?? {})) { const o=document.createElement('option'); o.value=key; o.textContent=spec.label; specSelect.append(o); }
  if ([...specSelect.options].some(o=>o.value===previousSpec)) specSelect.value=previousSpec;
  const specializationKey=specSelect.value;
  const species=EOTE_SPECIES[speciesKey];
  const previous={}; if (!resetCharacteristics) for (const i of $('customCharacteristics').querySelectorAll('input[data-characteristic]')) previous[i.dataset.characteristic]=Number(i.value);
  renderCreatorCharacteristics(speciesKey,previous);
  const careerCount=species?.careerFreeCount ?? 4; const specCount=species?.specFreeCount ?? 2;
  $('careerFreeHint').textContent=`Carreira: escolha exatamente ${careerCount} perícias para receber 1 grau gratuito.`;
  populateSkillChecks('careerFreeSkills',career?.skills ?? [],'careerFree');
  const spec=career?.specializations?.[specializationKey];
  $('specializationFreeHint').textContent=`Especialização: escolha exatamente ${specCount} perícias para receber 1 grau gratuito. Sobreposição pode elevar uma perícia a grau 2.`;
  populateSkillChecks('specializationFreeSkills',spec?.skills ?? [],'specFree');
  renderSpeciesChoice(speciesKey,careerKey,specializationKey);
  updateCreationPreview();
}

function updateCreationPreview() {
  try {
    const preview=eoteCreationPreview(creatorSelections());
    if (!preview) return;
    $('customCreationPreview').textContent=`PE inicial ${preview.xpBudget} · Características ${preview.characteristicCost} PE · restam ${preview.xpRemaining} PE · Obligation ${preview.obligation} · créditos ${preview.credits}.${preview.obligationOfficial?'':' Obligation solo é política local do VTT.'}`;
    $('createCustomCharacter').disabled=preview.xpRemaining<0;
  } catch(e) {
    $('customCreationPreview').textContent=e.message;
    $('createCustomCharacter').disabled=true;
  }
}

function initCharacterCreator() {
  const species=$('customSpecies'); species.innerHTML='';
  for (const [key,value] of Object.entries(EOTE_SPECIES)) { const o=document.createElement('option'); o.value=key; o.textContent=value.label; species.append(o); }
  const career=$('customCareer'); career.innerHTML='';
  for (const [key,value] of Object.entries(EOTE_CAREERS)) { const o=document.createElement('option'); o.value=key; o.textContent=value.label; career.append(o); }
  const obligation=$('customObligationType'); obligation.innerHTML=''; for(const value of OBLIGATION_TYPES_PT_BR){ const o=document.createElement('option'); o.value=value; o.textContent=value; obligation.append(o); }
  updateCharacterCreator({resetCharacteristics:true});
  for (const id of ['customSpecies','customCareer']) $(id).addEventListener('change',()=>updateCharacterCreator({resetCharacteristics:id==='customSpecies'}));
  $('customSpecialization').addEventListener('change',()=>{
    const species=EOTE_SPECIES[$('customSpecies').value]; const career=EOTE_CAREERS[$('customCareer').value]; const spec=career?.specializations?.[$('customSpecialization').value];
    populateSkillChecks('specializationFreeSkills',spec?.skills ?? [],'specFree');
    $('specializationFreeHint').textContent=`Especialização: escolha exatamente ${species?.specFreeCount ?? 2} perícias para receber 1 grau gratuito. Sobreposição pode elevar uma perícia a grau 2.`;
    renderSpeciesChoice($('customSpecies').value,$('customCareer').value,$('customSpecialization').value); updateCreationPreview();
  });
  for (const id of ['customObligation','obligationXp5','obligationXp10','obligationCredits','obligationCredits2500','customSpeciesVariant']) $(id).addEventListener('input',updateCreationPreview);
  $('customCharacteristics').addEventListener('input',updateCreationPreview);
}

function render() {
  const s = game.state;
  $('sceneTitle').textContent = s.scene.title;
  $('narration').textContent = s.scene.narration;
  $('campaignName').textContent = s.name;
  $('eventCount').textContent = `${s.events.length} evento${s.events.length===1?'':'s'}`;
  $('masterStatus').textContent = s.settings?.masterProvider==='http'
    ? 'Mestre remoto configurado · regras locais protegem a resolução'
    : 'Mestre local: índice de regras verificadas pronto; IA livre ainda não conectada';
  renderSession(s);
  renderMasterSettings(s.settings);
  renderStoredRuleResults(s.master);
  if (s.lastCheck) renderRoll(s.lastCheck);
  renderCharacter(s.player.character);
  renderProgression(s.player.character);
  renderTalents(s.player.character);
  renderEquipmentCatalog(s.player.character);
  renderFringeGroup(s);
  renderKnownFactions(s.world);
  renderKnownAftermaths(s.world);
  renderSkillSelect(s.player.character);
  renderCombat(s.player.character, s.combat);
  renderVehicle(s);
  renderSWE02OldLylek(s);
  renderLibrary();
}

function renderSWE02OldLylekMap(state){
  const host=$('swe02EncounterMap'),combat=state.combat,map=combat?.swe02OldLylekMap;
  const visible=Boolean(combat?.active&&combat.publishedAdventureContext?.adventureId==='long-arm-of-the-hutt'&&combat.publishedAdventureContext?.encounterId==='lylek-den-bounty-hunters'&&map?.schema===1);
  if(!host)return;host.hidden=!visible;if(!visible)return;
  const actorSelect=$('swe02MapActor'),grid=$('swe02MapGrid'),feedback=$('swe02MapFeedback'),previous=actorSelect.value;
  actorSelect.replaceChildren();
  for(const [key,actor] of Object.entries(map.actors??{})){const option=document.createElement('option');option.value=key;option.textContent=`${actor.kind==='pc'?'PC':'Caçador'}: ${actor.label}`;actorSelect.append(option);}
  if(Object.hasOwn(map.actors??{},previous))actorSelect.value=previous;
  grid.replaceChildren();
  for(const zone of map.zones??[]){
    const card=document.createElement('div');card.className=`swe02-map-zone swe02-zone-${zone.id}`;
    const heading=document.createElement('strong');heading.textContent=zone.label;card.append(heading);
    const occupants=Object.entries(map.actors??{}).filter(([key])=>map.actorLocations?.[key]===zone.id).map(([,actor])=>`${actor.kind==='pc'?'PC':'Caçador'}: ${actor.label}`);
    const occupantsLine=document.createElement('span');occupantsLine.className='swe02-map-occupants';occupantsLine.textContent=occupants.length?occupants.join(' · '):'Sem ator atribuído';card.append(occupantsLine);
    const assign=document.createElement('button');assign.type='button';assign.textContent='Atribuir ator selecionado';assign.disabled=!actorSelect.value;
    assign.addEventListener('click',()=>{try{const result=game.setSWE02OldLylekMapLocation({actorKey:actorSelect.value,locationId:zone.id});if(!result.ok)throw Error(result.message);$('swe02MapFeedback').textContent=`${map.actors[actorSelect.value]?.label??'Ator'} → ${zone.label}. Posição salva.`;render();}catch(e){$('swe02MapFeedback').textContent=e.message;}});
    card.append(assign);grid.append(card);
  }
  const last=map.moves?.at(-1);if(last){const actor=map.actors?.[last.actorKey],zone=map.zones?.find(x=>x.id===last.locationId);feedback.textContent=`Última posição salva: ${actor?.label??last.actorKey} → ${zone?.label??last.locationId}.`;}else feedback.textContent='Posições iniciais registradas: PCs no início da Heroes’ Journey; caçadores junto à boca da caverna (SWE02 p.16).';
}

function renderSWE02OldLylek(state){
  const p=state.world?.publishedAdventure,o=p?.oldLylek,host=$('swe02OldLylek');if(!host)return;
  const encounterActive=p?.id==='long-arm-of-the-hutt'&&p.status==='ACTIVE'&&p.currentNodeId==='lylek-den-ambush'&&o;
  const arrival=p?.id==='long-arm-of-the-hutt'&&p.status==='ACTIVE'&&p.currentNodeId==='new-meen-miners'&&o?.arrivalReceipt;
  host.hidden=!(encounterActive||arrival);
  if(arrival){
    $('swe02EncounterMap').hidden=true;
    $('swe02OldLylekStatus').textContent='Chegada a New Meen após cerca de uma milha: moradores cautelosos reconhecem B’ura, recebem o grupo como heróis, oferecem abrigo e rações (SWE02 p.18).';
    const details=o.arrivalReceipt.developerDetails;
    $('swe02ArrivalDetails').hidden=details?.status==='SHARED_AFTER_ASK';
    const factsHost=$('swe02ArrivalDeveloperFacts');factsHost.hidden=details?.status!=='SHARED_AFTER_ASK';factsHost.textContent=details?.facts?`${details.facts.contractor} ${details.facts.underworld} ${details.facts.identity}`:'';
    for(const id of ['swe02PcRoster','swe02CrewConfirm','swe02TrexHistoryControls','swe02LylekSlow','swe02LylekFast','swe02LylekCheck','swe02LylekProceed','swe02LylekFoot','swe02LylekRuse','swe02TacticsControls','swe02EncounterMap','swe02LylekCrash','swe02RepairReductionLabel','swe02LylekHunters','swe02PlayerCoverLabel','swe02TargetCoverLabel','swe02LylekRetreat','swe02LylekInsist','swe02LylekLoot','swe02LylekBeast','swe02LylekSubmissionLabel','swe02LylekFinish','swe02LylekContinue'])$(id).hidden=true;
    return;
  }
  $('swe02ArrivalDetails').hidden=true;$('swe02ArrivalDeveloperFacts').hidden=true;
  if(!encounterActive){$('swe02EncounterMap').hidden=true;return;}
  const phase=o.phase,combatId=state.combat?.publishedAdventureContext?.encounterId;
  renderSWE02OldLylekMap(state);
  const trex=o.trexCarryover,pcCount=Number(state.session?.campaignRules?.destiny?.playerCount??state.session?.eoteRules?.destiny?.playerCount??state.session?.playerCount);
  $('swe02OldLylekStatus').textContent=`Etapa: ${phase} · rota: ${o.route||'não escolhida'}${o.lookout?.spottedGand?' · Gand avistado (SWE02 p.17)':''}${o.lookout&&!o.lookout.spottedGand?' · Gand não avistado (SWE02 p.17)':''}${o.route==='fast'?' · caçadores recebem 1 Setback na iniciativa Cool (SWE02 p.17)':''}${o.lylekTriggered?' · ninho perturbado':''}${o.huntersOutcome?` · caçadores: ${o.huntersOutcome}`:''}${o.hunterLoot?.status==='available'?' · speeder dos caçadores pode ser inspecionado':o.hunterLoot?.status==='found'?' · encontrados: 2 stimpacks, medpac, kit de reparo (SWE02 p.18)':''}${o.ruse?` · hail: “${o.ruse.hail}”; sem uniformes imperiais; identificação perguntada: ${o.ruse.identificationAsked?'sim':'não'}; T’rex reconhecido: ${o.ruse.identifiedByHunters?.includes('T’rex')?'sim':'não'}; Oskara reconhece humanos: ${o.ruse.oskaraIdentifiesHumanHunters?'sim':'não'}`:''}${o.arrivalReceipt?' · chegada a ~1 milha: boas-vindas, convite e rações registrados':''}${trex?.status!=='selected'?' · histórico de T’rex precisa ser selecionado':trex.present?` · T’rex presente (${trex.history==='defeated_unconfirmed_dead'?'começa com 10 ferimentos':'sem ferimentos prévios'})`:' · T’rex morto e ausente'}${trex?.present&&pcCount===1?' · composição Gand + T’rex para 1 PC PENDENTE':''}`;
  const rosterHost=$('swe02PcRoster');if(rosterHost){rosterHost.replaceChildren();for(const entry of state.player?.roster?.entries??[]){const label=document.createElement('label'),check=document.createElement('input');check.type='checkbox';check.dataset.swe02PcId=entry.id;check.checked=Boolean(o.crewPcIds?.includes(entry.id));check.disabled=phase!=='approach';label.append(check,document.createTextNode(` ${entry.character?.name??entry.id}${entry.id===state.player?.roster?.activeCharacterId?' · ficha ativa':''}`));rosterHost.append(label);}}
  $('swe02TrexHistoryControls').hidden=trex?.status==='selected';
  $('swe02LylekSlow').hidden=phase!=='approach';$('swe02LylekFast').hidden=phase!=='approach';
  $('swe02LylekCheck').hidden=!['pilot-1','pilot-2','lookout','repair','repair-failed'].includes(phase);
  $('swe02LylekProceed').hidden=!['lookout','bluff'].includes(phase);$('swe02LylekFoot').hidden=!['lookout','bluff'].includes(phase);
  for(const id of ['swe02RuseRecognizedLabel','swe02OskaraPresentLabel','swe02OskaraRecognizesLabel','swe02LylekRuse'])$(id).hidden=phase!=='ruse';
  $('swe02TacticsControls').hidden=phase!=='combat-ready';
  $('swe02LylekCrash').hidden=phase!=='crash-checks';
  $('swe02RepairReductionLabel').hidden=!['repair','repair-failed'].includes(phase);
  $('swe02LylekHunters').hidden=phase!=='combat-ready'||trex?.status!=='selected'||!o.tactics;
  $('swe02LylekInsist').hidden=phase!=='post-hunters'||!o.lylekTriggered;
  $('swe02LylekLoot').hidden=o.hunterLoot?.status!=='available';
  $('swe02LylekBeast').hidden=phase!=='lylek-warned';
  $('swe02LylekFinish').hidden=!(state.combat?.active&&combatId==='lylek-den-lylek');
  $('swe02LylekSubmissionLabel').hidden=!(state.combat?.active&&combatId==='lylek-den-lylek');
  $('swe02PlayerCoverLabel').hidden=!(state.combat?.active&&combatId==='lylek-den-bounty-hunters');
  $('swe02PlayerBehindRocks').checked=Boolean(state.combat?.swe02OldLylekCover?.playerBehindRockPile);
  $('swe02TargetCoverLabel').hidden=!(state.combat?.active&&combatId==='lylek-den-bounty-hunters'&&Boolean($('targetAdversary')?.value));
  $('swe02LylekRetreat').hidden=!(state.combat?.active&&combatId==='lylek-den-bounty-hunters'&&o.tactics?.approach==='frontal'&&o.tactics?.frontalOutcome==='fighting_retreat');
  $('swe02LylekContinue').hidden=!['post-hunters','post-lylek','lylek-warned','lylek-ready'].includes(phase)||!p.flags?.lylekHuntersResolved;
  const locked=Boolean(state.combat?.active);
  for(const id of ['swe02LylekSlow','swe02LylekFast','swe02LylekProceed','swe02LylekFoot','swe02LylekCrash','swe02LylekHunters','swe02LylekInsist','swe02LylekBeast','swe02LylekContinue','swe02LylekLoot','swe02LylekRetreat'])$(id).disabled=locked&&id!=='swe02LylekRetreat';
}


function renderKnownFactions(world={}) {
  const host=$('knownFactions');
  if(!host) return;
  host.innerHTML='';
  const factions=Object.values(world?.factions??{}).filter(f=>Boolean(f.knownToPlayer));
  if(!factions.length){ host.innerHTML='<div class="result empty">Nenhuma facção conhecida ainda.</div>'; return; }
  factions.sort((a,b)=>String(a.name).localeCompare(String(b.name),'pt-BR'));
  for(const faction of factions){
    const card=document.createElement('div'); card.className='result';
    const title=document.createElement('strong'); title.textContent=faction.name; card.append(title);
    const publicFacts=(faction.facts??[]).filter(f=>f.knownToPlayer).map(f=>f.text);
    const detail=document.createElement('small'); detail.textContent=publicFacts.length?publicFacts.join(' • '):'Nenhum fato público registrado.'; card.append(detail);
    const publicPlans=(faction.plans??[]).filter(p=>p.knownToPlayer);
    for(const plan of publicPlans){ const visibleSteps=(plan.steps??[]).filter(s=>s.knownToPlayer||s.status==='completed'); const active=visibleSteps.find(s=>s.status==='active'); const line=document.createElement('small'); line.textContent=`Plano: ${plan.title} · ${plan.status==='completed'?'concluído':plan.status==='active'?'em andamento':plan.status==='aborted'?'abortado':'planejado'}${active?` · etapa atual: ${active.text}`:''}`; card.append(line); }
    host.append(card);
  }
}

function renderKnownAftermaths(world={}) {
  const host=$('knownAftermaths'); if(!host) return; host.innerHTML='';
  const items=(world?.aftermaths??[]).filter(a=>a.status==='active'&&a.knownToPlayer);
  if(!items.length){ host.innerHTML='<div class="result empty">Nenhuma consequência duradoura conhecida.</div>'; return; }
  items.sort((a,b)=>(a.createdAtTick??0)-(b.createdAtTick??0));
  for(const item of items){
    const card=document.createElement('div'); card.className='result';
    const title=document.createElement('strong'); title.textContent=item.summary || 'Consequência persistente'; card.append(title);
    const detail=document.createElement('small'); detail.textContent=`Desfecho: ${String(item.outcome||'registrado').replaceAll('_',' ')}${item.planId?' · ligado a um plano de facção':''}`; card.append(detail);
    host.append(card);
  }
}

function renderSession(state) {
  const session=state.session;
  const hasCharacter=Boolean(state.player?.character);
  const active=session?.status==='ativa';
  $('sessionStatus').textContent=active
    ? `Sessão ${session.number} · ${SESSION_PHASE_LABELS_PT_BR[session.phase] ?? session.phase}`
    : session?.status==='encerrada'
      ? `Sessão ${session.number} encerrada`
      : 'Nenhuma sessão iniciada';
  $('startSession').hidden=active;
  $('startSession').disabled=!hasCharacter || state.combat?.active;
  $('closeSession').hidden=!active;
  $('closeSession').disabled=Boolean(state.combat?.active);
  $('sessionHistoryCount').textContent=`${state.sessionHistory?.length ?? 0} sessão(ões) encerrada(s)`;
  const prompt=$('masterPrompt');
  const suggested=$('suggestedCheckCard');
  const question=state.master?.pendingQuestion;
  prompt.hidden=!question;
  prompt.textContent=question ? `Mestre: ${question}` : '';
  const check=state.master?.suggestedCheck;
  suggested.hidden=!check;
  if (check) {
    const label=SKILL_LABELS_PT_BR[check.skillKey] ?? check.skillKey;
    $('suggestedCheckText').textContent=`${label} · Dificuldade ${check.difficulty}${check.challenge?` · Desafio ${check.challenge}`:''}${check.boost?` · Impulso ${check.boost}`:''}${check.setback?` · Contratempo ${check.setback}`:''}${check.reason?` · ${check.reason}`:''}`;
  }
}

function renderMasterSettings(settings={}) {
  const mode=settings.masterProvider==='http'?'http':'none';
  $('masterProviderMode').value=mode;
  $('masterEndpointWrap').hidden=mode!=='http';
  $('masterEndpoint').value=settings.masterEndpoint ?? '';
  $('ttsEnabled').checked=Boolean(settings.ttsEnabled);
  $('ttsRate').value=String(settings.ttsRate ?? 1);
  if (![...$('ttsRate').options].some(o=>o.value===$('ttsRate').value)) $('ttsRate').value='1';
}

function renderStoredRuleResults(master) {
  if (!master?.lastRuleQuery || !Array.isArray(master.lastRuleResults) || !master.lastRuleResults.length) return;
  $('ruleQuery').value=master.lastRuleQuery;
  renderRuleResults({query:master.lastRuleQuery,grounded:master.lastRuleResults.some(r=>['TESTADO','IMPLEMENTADO','FONTE_IMPORTADA'].includes(r.status)&&r.summary&&r.summary!=='A_AUDITAR'),results:master.lastRuleResults,retrieval:{authority:master.lastRuleAuthority ?? null}});
}

function renderRuleResults(result) {
  const host=$('ruleResults'); host.innerHTML='';
  if (!result.results?.length) {
    host.innerHTML='<div class="result fail">Nenhuma regra pertinente foi encontrada no corpus auditado.</div>';
    $('ruleQueryStatus').textContent='Sem evidência local suficiente.';
    return;
  }
  const authority=result.retrieval?.authority;
  $('ruleQueryStatus').textContent=result.grounded
    ? `Consulta fundamentada · ${result.results.length} resultado(s).${authority?.precedenceApplied?' Ordem de autoridade aplicada.':''}`
    : 'Atenção: o resultado principal ainda é uma lacuna não auditada.';
  for (const rule of result.results) {
    const card=document.createElement('article'); card.className=`rule-result-card ${['TESTADO','IMPLEMENTADO','FONTE_IMPORTADA'].includes(rule.status)?'grounded':'pending'}`;
    const sourcePage=rule.page!==null&&rule.page!==undefined ? ` · pág. ${rule.page}` : '';
    const badge=rule.status==='FONTE_IMPORTADA' ? `PDF · ${libraryAuthorityTypeLabel(rule.authorityType)}` : rule.official===false ? 'Política local do Mestre' : rule.status;
    const summary=rule.summary==='A_AUDITAR'?'Ainda não há evidência auditada suficiente para esta regra.':rule.summary;
    card.innerHTML=`<div class="rule-result-head"><strong>${escapeHtml(rule.title)}</strong><span>${escapeHtml(badge)}</span></div><p>${escapeHtml(summary)}</p><small>${escapeHtml(rule.source ?? 'Fonte não registrada')}${escapeHtml(sourcePage)}</small>`;
    host.append(card);
  }
}

function formatBytes(value=0) {
  const bytes=Number(value)||0;
  if (bytes < 1024*1024) return `${Math.max(1,Math.round(bytes/1024))} KB`;
  return `${(bytes/(1024*1024)).toFixed(bytes>100*1024*1024?0:1)} MB`;
}

function renderLibrary() {
  const available=nativeLibraryAvailable();
  $('importLibraryPdf').disabled=!available;
  $('searchLibrary').disabled=!available;
  $('libraryQuery').disabled=!available;
  $('libraryEnvironmentStatus').textContent=available
    ? 'Modo Android: PDFs ficam anexados no armazenamento privado e o índice é consultável sem internet.'
    : 'Preview de navegador: a Biblioteca pode ser visualizada, mas importar/indexar PDF exige o aplicativo Android com a ponte nativa.';
  const docs=listLibraryDocuments();
  $('libraryDocumentCount').textContent=String(docs.length);
  $('libraryIndexedCount').textContent=String(docs.filter(d=>d.status==='INDEXADO').length);
  const host=$('libraryDocuments'); host.innerHTML='';
  if (!docs.length) { host.innerHTML='<div class="result empty">Nenhum PDF importado.</div>'; return; }
  for (const doc of docs) {
    const row=document.createElement('article'); row.className='library-row';
    const pages=Number(doc.pageCount||0); const indexed=Number(doc.indexedPages||0);
    const authority=String(doc.authorityType || 'SUPLEMENTO').toUpperCase();
    const enabled=doc.enabled!==false;
    row.dataset.docId=doc.docId;
    row.innerHTML=`<div class="library-row-main"><strong>${escapeHtml(doc.title || doc.fileName || 'PDF importado')}</strong><span>${escapeHtml(libraryStatusLabel(doc.status))} · ${pages} pág. · ${formatBytes(doc.sizeBytes)}</span><small>${indexed}/${pages || 0} páginas com texto · armazenamento privado offline</small><div class="library-policy"><label>Autoridade<select class="library-authority" aria-label="Autoridade de ${escapeHtml(doc.title || doc.fileName)}"><option value="ERRATA">Errata / correção</option><option value="BASE">Livro básico</option><option value="SUPLEMENTO">Complemento de regras</option><option value="AVENTURA">Aventura / cenário</option><option value="REFERENCIA">Referência</option></select></label><label class="library-enabled-label"><input class="library-enabled" type="checkbox" ${enabled?'checked':''}> Usar no Mestre</label></div>${doc.lastError?`<small class="library-error">Último erro: ${escapeHtml(doc.lastError)}</small>`:''}</div><div class="library-actions"><button class="library-reindex" data-doc-id="${escapeHtml(doc.docId)}">Reindexar</button><button class="library-remove" data-doc-id="${escapeHtml(doc.docId)}" aria-label="Remover ${escapeHtml(doc.title || doc.fileName)}">Remover</button></div>`;
    row.querySelector('.library-authority').value=['ERRATA','BASE','SUPLEMENTO','AVENTURA','REFERENCIA'].includes(authority)?authority:'SUPLEMENTO';
    host.append(row);
  }
}

function renderLibrarySearch(results=[]) {
  const host=$('librarySearchResults'); host.innerHTML='';
  if (!results.length) { host.innerHTML='<div class="result fail">Nenhum trecho encontrado nos PDFs indexados.</div>'; return; }
  for (const result of results) {
    const card=document.createElement('article'); card.className='rule-result-card grounded';
    const page=result.page ? ` · pág. ${result.page}` : '';
    card.innerHTML=`<div class="rule-result-head"><strong>${escapeHtml(result.title)}</strong><span>${escapeHtml(libraryAuthorityTypeLabel(result.authorityType))}</span></div><p>${escapeHtml(result.summary)}</p><small>${escapeHtml(result.source)}${escapeHtml(page)}</small>`;
    host.append(card);
  }
}

function renderRoll(r) {
  const el = $('rollResult');
  const state = r.success ? 'SUCESSO' : 'FALHA';
  const second = r.netAdvantage > 0 ? `Vantagem +${r.netAdvantage}` : r.netAdvantage < 0 ? `Ameaça ${Math.abs(r.netAdvantage)}` : 'Sem Vantagem/Ameaça líquida';
  const specials = [r.triumph ? `Triunfo ${r.triumph}` : '', r.despair ? `Desesperação ${r.despair}` : ''].filter(Boolean).join(' · ');
  el.className = `result ${r.success ? 'success' : 'fail'}`;
  const expert=r.expertTalent?.used ? ` · ${r.expertTalent.label}: -1 Dificuldade, 2 Tensão` : '';
  el.textContent = `${state} · Êxito líquido ${r.netSuccess} · ${second}${specials ? ` · ${specials}` : ''}${expert}`;
}

function playerInitiativeReady(combat) {
  if (!combat?.active) return false;
  if (!combat.encounter?.slots?.length) return true;
  const slot=currentSlot(combat);
  return Boolean(slot?.side==='player' && combat.encounter.waitingForPlayer);
}

function renderSkillSelect(character) {
  const select = $('skillSelect');
  select.innerHTML = '';
  if (!character) {
    const opt=document.createElement('option'); opt.value=''; opt.textContent='Sem personagem carregado'; select.append(opt); select.disabled=true;
    $('checkHint').textContent='Carregue um personagem para usar suas perícias automaticamente.';
    renderExpertTalentOption(null);
    return;
  }
  select.disabled=false;
  const keys = Object.keys(character.skills).filter(k => k in SKILLS);
  keys.sort((a,b)=>(SKILL_LABELS_PT_BR[a]??a).localeCompare(SKILL_LABELS_PT_BR[b]??b,'pt-BR'));
  for (const key of keys) {
    const opt=document.createElement('option');
    const characteristic=CHARACTERISTIC_LABELS_PT_BR[SKILLS[key]] ?? SKILLS[key];
    opt.value=key; opt.textContent=`${SKILL_LABELS_PT_BR[key] ?? key} ${character.skills[key]} · ${characteristic} ${character.characteristics[SKILLS[key]]}`;
    select.append(opt);
  }
  $('checkHint').textContent=game.state.combat.active
    ? 'Em combate, usar uma perícia consome a ação da sua posição de iniciativa.'
    : 'A reserva é montada automaticamente a partir da característica e do grau da perícia da ficha.';
  renderExpertTalentOption(character);
}

function renderExpertTalentOption(character) {
  const row=$('expertTalentRow');
  if(!row) return;
  const skill=$('skillSelect').value;
  const config=skill==='astronavegacion' ? {original:'MASSTAR',label:'Navegador mestre'} : null;
  const available=Boolean(character&&config&&game.talentRank(config.original)>0);
  row.hidden=!available;
  $('useExpertTalent').checked=available ? $('useExpertTalent').checked : false;
  if(available) $('expertTalentLabel').textContent=`Usar ${config.label}: sofrer 2 de Tensão e reduzir a Dificuldade em 1 (mín. Fácil)`;
}

function selectedWeapon(character) {
  if (!character) return null;
  return character.weapons?.[Number($('weaponSelect').value)] ?? character.weapons?.[0] ?? null;
}

function selectedAdversary() {
  const id=$('targetAdversary')?.value;
  return game.state.combat.adversaries?.find(a=>a.id===id) ?? null;
}

function renderAttackRangeControls(character) {
  const weapon=selectedWeapon(character);
  const rangeSelect=$('targetRange');
  const previous=rangeSelect.value;
  rangeSelect.innerHTML='';
  if (!weapon) {
    $('attackDifficultyText').textContent='Nenhuma arma disponível.';
    return;
  }

  const adversary=selectedAdversary();
  if (game.state.combat.encounter && adversary) {
    const opt=document.createElement('option'); opt.value=adversary.range; opt.textContent=adversary.range; rangeSelect.append(opt);
    rangeSelect.disabled=true;
    try {
      const difficulty=attackDifficultyForWeapon(weapon, adversary.range);
      const upgrades=upgradeDifficultyForAdversaryTalent(adversary);
      $('attackDifficultyText').textContent=upgrades>0
        ? `Dificuldade base ${difficulty} · Adversário ${upgrades} melhora a dificuldade · alcance ${adversary.range}.`
        : `Dificuldade automática: ${difficulty} · alcance ${adversary.range}.`;
    } catch(e) { $('attackDifficultyText').textContent=e.message; }
    return;
  }

  rangeSelect.disabled=false;
  const ranges=availableTargetRanges(weapon);
  for (const range of ranges) {
    const opt=document.createElement('option'); opt.value=range; opt.textContent=range; rangeSelect.append(opt);
  }
  const preferred=[previous, weapon.range, 'Médio', 'Curto', 'Interação'].find(v=>ranges.includes(v));
  if (preferred) rangeSelect.value=preferred;
  try {
    const difficulty=attackDifficultyForWeapon(weapon, rangeSelect.value);
    $('attackDifficultyText').textContent=`Dificuldade automática: ${difficulty} · alcance ${rangeSelect.value}.`;
  } catch(e) { $('attackDifficultyText').textContent=e.message; }
}

function stimpackQuantity(character) {
  if (!character) return 0;
  const item=character.gear.find(g=>/injetor(?:es)? de estimulantes?/i.test(g.name));
  return Number(item?.quantity ?? 0);
}

function renderCharacter(character) {
  $('noCharacter').hidden=Boolean(character);
  $('characterData').hidden=!character;
  if (!character) return;
  const health=getHealthStatus(character);
  $('characterName').textContent=character.name;
  $('characterMeta').textContent=[character.species,character.profession].filter(Boolean).join(' · ') || 'Ficha local';
  $('xpAvailable').textContent=character.xp.available;
  $('soakValue').textContent=character.derived.soak;
  $('woundsValue').textContent=`${character.derived.wounds.current}/${character.derived.wounds.threshold}`;
  $('strainValue').textContent=`${character.derived.strain.current}/${character.derived.strain.threshold}`;
  $('healthStatusValue').textContent=health.label;
  $('creditsValue').textContent=character.credits;
  $('criticalsValue').textContent=character.criticalInjuries.length;

  const stats=$('characteristics'); stats.innerHTML='';
  for (const [key,value] of Object.entries(character.characteristics)) {
    const item=document.createElement('div'); item.innerHTML=`<span>${CHARACTERISTIC_LABELS_PT_BR[key] ?? key}</span><strong>${value}</strong>`; stats.append(item);
  }

  const skills=$('trainedSkills'); skills.innerHTML='';
  const trained=Object.entries(character.skills).filter(([,rank])=>rank>0).sort((a,b)=>b[1]-a[1] || (SKILL_LABELS_PT_BR[a[0]]??a[0]).localeCompare(SKILL_LABELS_PT_BR[b[0]]??b[0],'pt-BR'));
  for (const [key,rank] of trained) {
    const row=document.createElement('div'); row.className='skill-row'; row.innerHTML=`<span>${SKILL_LABELS_PT_BR[key] ?? key}</span><strong>${rank}</strong>`; skills.append(row);
  }
  if (!trained.length) skills.textContent='Nenhuma perícia treinada.';

  const gear=$('gearList'); gear.innerHTML='';
  for (const w of character.weapons) {
    const extras=[`Dano ${w.damage}`,w.range,w.crit?`Crítico ${w.crit}`:'',w.pierce?`Perfuração ${w.pierce}`:'',w.damageType==='strain'?'Atordoante':''].filter(Boolean).join(' · ');
    const row=document.createElement('div'); row.className='gear-row'; row.innerHTML=`<span>${w.name}</span><small>${extras}</small>`; gear.append(row);
  }
  for (const g of character.gear) {
    const row=document.createElement('div'); row.className='gear-row'; row.innerHTML=`<span>${g.name}</span><small>${g.quantity && g.quantity>1 ? `x${g.quantity}` : g.quantity===0 ? 'Esgotado' : 'Equipamento'}</small>`; gear.append(row);
  }
  $('sourceNote').textContent=character.source?.type==='eote_test_fixture' ? 'Fixture local de teste EotE; não é personagem oficial nem fonte de regra.' : character.source?.type==='custom_aor_core' ? 'Ficha legada de Age of Rebellion preservada apenas para migração; Duty não é mecânica ativa no perfil EotE.' : 'Ficha local.';
}

function renderTalents(character) {
  const host=$('talentOptions'); if(!host) return; host.innerHTML='';
  if(!character){ host.innerHTML='<div class="result empty">Carregue um personagem.</div>'; return; }
  const options=game.availableTalents();
  if(!options.length){ host.innerHTML='<div class="result empty">Esta ficha não possui uma especialização inicial mapeada para compras automáticas de talentos.</div>'; return; }
  const ranks=game.talentRanks();
  for(let tier=1;tier<=5;tier++){
    const tierNodes=options.filter(t=>t.row===tier);
    if(!tierNodes.length) continue;
    const title=document.createElement('div'); title.className='talent-tier'; title.textContent=`Linha ${tier} · ${tier*5} PE por espaço`; host.append(title);
    for(const t of tierNodes){
      const status=t.purchased?'Adquirido':!t.unlocked?'Bloqueado':!t.available?'PE insuficientes':'Disponível';
      const row=document.createElement('div'); row.className=`talent-row ${t.purchased?'purchased':t.unlocked?'unlocked':'locked'}`;
      const choice=t.needsChoice&&!t.purchased ? `<select class="talent-characteristic" aria-label="Característica para Dedicação">${Object.entries(CHARACTERISTIC_LABELS_PT_BR).map(([key,label])=>`<option value="${escapeHtml(key)}">${escapeHtml(label)}</option>`).join('')}</select>` : '';
      row.innerHTML=`<div><strong>${escapeHtml(t.name)}</strong><small>Linha ${t.row} · coluna ${t.column} · ${t.cost} PE · ${status}${ranks[t.name]?` · grau atual ${ranks[t.name]}`:''}</small>${choice}</div><button class="secondary buy-talent" data-talent-id="${escapeHtml(t.id)}" ${!t.available?'disabled':''}>${t.purchased?'Adquirido':t.unlocked?'Comprar':'Bloqueado'}</button>`;
      host.append(row);
    }
  }
}

function renderEquipmentCatalog(character){
  const weaponsHost=$('catalogWeapons'), armorHost=$('catalogArmor'); if(!weaponsHost||!armorHost) return;
  weaponsHost.innerHTML=''; armorHost.innerHTML='';
  const catalog=game.equipmentCatalog();
  const pending=game.state.master?.pendingAcquisition ?? null;
  for(const item of catalog.weapons){
    const row=document.createElement('div'); row.className='catalog-row';
    const restriction=item.restricted?' · Restrita':'';
    const damage=item.damageBonus!==null&&item.damageBonus!==undefined?`Fortaleza ${item.damageBonus>=0?'+':''}${item.damageBonus}`:`${item.damage}`;
    row.innerHTML=`<div><strong>${escapeHtml(item.name)}</strong><small>Dano ${escapeHtml(damage)} · Crítico ${item.crit??'—'} · ${escapeHtml(item.range)} · ${item.price.toLocaleString('pt-BR')} créditos · raridade ${item.rarity}${restriction}</small>${item.qualities?.length?`<small>${escapeHtml(item.qualities.join(' · '))}</small>`:''}</div><button class="secondary propose-weapon" data-item-id="${escapeHtml(item.id)}" ${character&&!pending?'':'disabled'}>${pending?.itemId===item.id?'Aguardando Mestre':'Propor aquisição'}</button>`;
    weaponsHost.append(row);
  }
  if(pending) showFeedback('equipmentCatalogFeedback',`Aguardando o Mestre resolver: ${pending.itemName} · preço listado ${Number(pending.listedPrice).toLocaleString('pt-BR')} créditos · raridade ${pending.rarity}${pending.restricted?' · restrita':''}.`);
  else showFeedback('equipmentCatalogFeedback','');
  for(const item of catalog.armor){
    const row=document.createElement('div'); row.className='catalog-row reference';
    row.innerHTML=`<div><strong>${escapeHtml(item.name)}</strong><small>Defesa ${item.defense} · Proteção ${item.soak} · preço não consta nesta tabela-resumo</small></div>`;
    armorHost.append(row);
  }
}

function linesFromTextarea(id){ return $(id).value.split(/\r?\n/).map(v=>v.trim()).filter(Boolean); }
function renderFringeGroup(state){
  if(!$('groupBaseName')) return;
  const summary=game.groupSummary(); const g=summary.group;
  $('groupBaseName').value=g.base?.name??'Refúgio / Base';
  $('groupBaseLocation').value=g.base?.location??'';
  $('groupBaseDescription').value=g.base?.description??'';
  $('groupObligationTotal').textContent=String(summary.obligationTotal);
  $('groupResources').value=(g.resources??[]).join('\n');
  $('groupBelongings').value=(g.belongings??[]).join('\n');
  $('groupContacts').value=(g.contacts??[]).join('\n');
  $('groupDestinyLight').value=String(g.destiny?.light??0);
  $('groupDestinyDark').value=String(g.destiny?.dark??0);
  const host=$('groupObligationEntries'); host.innerHTML='';
  if(!(g.obligationEntries??[]).length){ host.innerHTML='<div class="result empty">Nenhuma Obligation do grupo registrada.</div>'; }
  for(const e of (g.obligationEntries??[])){
    const row=document.createElement('div'); row.className='gear-row group-obligation-row';
    row.innerHTML=`<span><strong>${escapeHtml(e.member||'Grupo')}</strong> · ${escapeHtml(e.type||'Obligation')} · ${Number(e.value)||0}</span><small>${escapeHtml(e.description||'Sem descrição')}</small><button class="icon-btn remove-group-obligation" data-obligation-id="${escapeHtml(e.id)}" aria-label="Remover Obligation">×</button>`;
    host.append(row);
  }
}

function renderProgression(character) {
  const select=$('progressSkill');
  const button=$('trainSkill');
  if (!character) {
    select.innerHTML='<option value="">Sem personagem</option>';
    select.disabled=true; button.disabled=true;
    $('progressCost').textContent='Carregue uma ficha para usar PE.';
    return;
  }
  const previous=select.value;
  select.innerHTML='';
  const keys=Object.keys(character.skills).filter(k=>Number.isInteger(character.skills[k]) && character.skills[k]<MAX_SKILL_RANK);
  keys.sort((a,b)=>(SKILL_LABELS_PT_BR[a]??a).localeCompare(SKILL_LABELS_PT_BR[b]??b,'pt-BR'));
  for (const key of keys) {
    const rank=character.skills[key];
    const career=isCareerSkill(character,key);
    let cost='—';
    try { cost=skillRankCostForCharacter(character,key); } catch {}
    const opt=document.createElement('option'); opt.value=key; opt.textContent=`${SKILL_LABELS_PT_BR[key] ?? key} ${rank}→${rank+1} · ${career?'profissional':'não profissional'} · ${cost} PE`; select.append(opt);
  }
  select.disabled=!keys.length;
  if ([...select.options].some(o=>o.value===previous)) select.value=previous;
  const selected=select.value;
  if (!selected) { $('progressCost').textContent='Todas as perícias disponíveis já estão no grau máximo.'; button.disabled=true; return; }
  const cost=skillRankCostForCharacter(character,selected);
  const career=isCareerSkill(character,selected);
  $('progressCost').textContent=`Próximo grau: ${cost} PE · ${career?'perícia profissional':'perícia não profissional'} · saldo ${character.xp.available} PE.`;
  button.disabled=character.xp.available<cost;
}

function renderEncounter(combat) {
  const panel=$('initiativePanel');
  const encounter=combat.encounter;
  const targetWrap=$('targetAdversaryWrap');
  if (!combat.active || !encounter?.slots?.length) {
    panel.hidden=true;
    targetWrap.hidden=true;
    $('targetAdversary').innerHTML='';
    return;
  }
  panel.hidden=false;
  targetWrap.hidden=false;
  const slot=currentSlot(combat);
  if (encounter.outcome==='player_incapacitated') $('initiativeStatus').textContent='Você está incapacitado';
  else if (encounter.outcome==='adversaries_defeated') $('initiativeStatus').textContent='Todos os adversários foram derrotados';
  else $('initiativeStatus').textContent=slot?.side==='player' ? 'Sua posição de iniciativa' : 'Posição de PNPC';
  $('initiativeSlots').textContent=encounter.slots.map((s,i)=>`${i===encounter.cursor?'[':''}${s.side==='player'?'PJ':'PNPC'}${i===encounter.cursor?']':''}`).join(' · ');

  const list=$('adversaryList'); list.innerHTML='';
  for (const adv of combat.adversaries ?? []) {
    const health=adversaryHealthStatus(adv);
    const row=document.createElement('div'); row.className=`adversary-row${health.incapacitated?' defeated':''}`;
    const left=document.createElement('div');
    const title=document.createElement('strong'); title.textContent=adv.name; left.append(title);
    const meta=document.createElement('span');
    const faction=adv.factionId ? game.state.world?.factions?.[adv.factionId] : null;
    const factionLabel=faction?.knownToPlayer ? ` · ${faction.name}` : '';
    meta.textContent=(adv.kind==='minion'
      ? `Lacaios ${aliveMinions(adv)}/${adv.membersTotal} · ${adv.range}`
      : `${adv.kind==='rival'?'Rival':'Antagonista'} · ${adv.range}`) + factionLabel;
    left.append(meta);
    const right=document.createElement('small'); right.textContent=`Ferimentos ${adv.wounds.current}/${adv.wounds.threshold} · ${health.label}`;
    row.append(left,right); list.append(row);
  }

  const target=$('targetAdversary');
  const previous=target.value;
  target.innerHTML='';
  for (const adv of combat.adversaries ?? []) {
    if (adversaryHealthStatus(adv).incapacitated) continue;
    const opt=document.createElement('option'); opt.value=adv.id; opt.textContent=adv.name; target.append(opt);
  }
  if ([...target.options].some(o=>o.value===previous)) target.value=previous;

  const log=$('npcTurnLog');
  const recent=(combat.log ?? []).filter(e=>e.type==='TURNO_NPC').slice(-3);
  log.textContent=recent.length ? recent.map(e=>e.summary).join(' • ') : 'Nenhum turno automático de PNPC executado ainda.';
}

function renderCombat(character, combat) {
  $('combatNoCharacter').hidden=Boolean(character);
  $('combatData').hidden=!character;
  if (!character) return;
  const health=getHealthStatus(character);
  const ready=playerInitiativeReady(combat);
  $('combatCharacterName').textContent=character.name;
  $('combatHealthLine').textContent=`${health.label} · Ferimentos ${character.derived.wounds.current}/${character.derived.wounds.threshold} · Tensão ${character.derived.strain.current}/${character.derived.strain.threshold}`;
  $('combatRound').textContent=combat.active ? combat.round : '—';
  $('combatActionState').textContent=combat.active ? (combat.playerTurn.actionUsed?'Usada':ready?'Disponível':'Aguardando') : 'Fora de combate';
  $('combatManeuverState').textContent=combat.active ? `${combat.playerTurn.maneuversUsed}/2` : '—';
  const quickRank=game.talentRank('REACCIÓN RÁPIDA');
  $('quickReactionStrain').max=String(quickRank);
  if(Number($('quickReactionStrain').value)>quickRank) $('quickReactionStrain').value=String(quickRank);
  $('quickReactionStrain').disabled=combat.active || health.unconscious || quickRank<=0;
  $('quickReactionHelp').textContent=quickRank>0 ? `Pode sofrer até ${quickRank} de Tensão para adicionar a mesma quantidade de dados de Impulso à iniciativa.` : 'Sem Reação rápida adquirida.';
  $('startEncounter').disabled=combat.active || health.unconscious;
  const pendingEncounter=game.state.master?.pendingEncounter;
  $('masterEncounterCard').hidden=!pendingEncounter || combat.active;
  if(pendingEncounter){
    const groups=pendingEncounter.adversaries.map(a=>`${a.count>1?`${a.count}× `:''}${game.adversaryProfiles()[a.profileKey]?.name ?? a.profileKey}`).join(', ');
    $('masterEncounterText').textContent=`${groups}${pendingEncounter.sourceHookId?' · consequência do mundo':''}. Você ainda escolhe se usa Reação rápida antes da iniciativa.`;
  }
  $('startPendingEncounter').disabled=!pendingEncounter || combat.active || health.unconscious;
  $('combatCause').hidden=!combat.causalHookSummary;
  $('combatCause').textContent=combat.causalHookSummary?`Consequência causal: ${combat.causalHookSummary}`:'';
  $('startCombat').disabled=combat.active || health.unconscious;
  $('nextTurn').disabled=!combat.active || health.unconscious || (combat.encounter && !ready);
  $('endCombat').disabled=!combat.active;
  renderEncounter(combat);

  $('normalManeuver').disabled=!combat.active || !ready || health.unconscious || combat.playerTurn.maneuversUsed>=2;
  $('strainManeuver').disabled=!combat.active || !ready || health.unconscious || combat.playerTurn.maneuversUsed!==1;
  $('actionManeuver').disabled=!combat.active || !ready || health.unconscious || combat.playerTurn.maneuversUsed!==1 || combat.playerTurn.actionUsed;
  $('aimButton').disabled=!combat.active || !ready || health.unconscious || combat.playerTurn.maneuversUsed>=2;
  $('aimStatus').textContent=Number(combat.pendingAttackBoost ?? 0)>0
    ? `Mirar preparado: +${combat.pendingAttackBoost} dado de Impulso no próximo ataque.`
    : 'Nenhum bônus de Mirar preparado.';

  const select=$('weaponSelect');
  const previous=select.value;
  select.innerHTML='';
  character.weapons.forEach((weapon,index)=>{
    const opt=document.createElement('option'); opt.value=String(index); opt.textContent=`${weapon.name} · Dano ${weapon.damage} · ${weapon.range}`; select.append(opt);
  });
  if ([...select.options].some(o=>o.value===previous)) select.value=previous;
  renderAttackRangeControls(character);
  $('attackButton').disabled=!character.weapons.length || health.unconscious || (combat.active && (!ready || combat.playerTurn.actionUsed));

  const last=combat.lastAttack;
  if (last) renderAttack(last);
  $('activateCritical').hidden=!combat.pendingPlayerCritical;
  $('activateCritical').disabled=!combat.pendingPlayerCritical;
  const stimHeal=Math.max(1,5-Math.max(0,Number(character?.stimpackUsesToday??0)||0));
  $('stimpackCount').textContent=`Injetores disponíveis: ${stimpackQuantity(character)}. Próximo uso cura até ${stimHeal} Ferimento${stimHeal===1?'':'s'} e consome 1 injetor.`;
  $('useStimpack').disabled=health.unconscious || stimpackQuantity(character)<=0 || (combat.active && (!ready || combat.playerTurn.maneuversUsed>=2));
  $('medicineButton').disabled=health.unconscious || (combat.active && (!ready || combat.playerTurn.actionUsed || combat.medicineUsed));
  $('restNightButton').disabled=combat.active;
}

function renderAttack(attack) {
  const el=$('attackResult');
  const context=[attack.targetName || '',attack.targetRange ? `Alcance ${attack.targetRange}` : '', Number.isFinite(attack.difficulty) ? `Dificuldade ${attack.difficulty}` : '', attack.challenge ? `Desafio ${attack.challenge}` : ''].filter(Boolean).join(' · ');
  if (!attack.hit) {
    const advantage=attack.check.netAdvantage>0?` · Vantagem ${attack.check.netAdvantage}`:attack.check.netAdvantage<0?` · Ameaça ${Math.abs(attack.check.netAdvantage)}`:'';
    el.className='result fail'; el.textContent=`FALHA${context?` · ${context}`:''}${advantage}`; return;
  }
  const parts=[`ACERTO · Dano ${attack.damage}`];
  if (context) parts.push(context);
  if (attack.pierce) parts.push(`Perfuração ${attack.pierce}`);
  if (attack.damageType==='strain') parts.push('Dano de Tensão');
  if (attack.quickStrikeBoost>0) parts.push(`Ataque rápido +${attack.quickStrikeBoost} Impulso`);
  if (attack.targetDamage) parts.push(`Alvo sofreu ${attack.targetDamage.suffered} Ferimento(s) após Proteção`);
  else parts.push('Dano deve ser reduzido pela Proteção do alvo');
  if (attack.criticalAvailable) parts.push(attack.criticalByTriumph ? 'Crítico disponível por Triunfo' : `Crítico disponível por ${attack.criticalCost} Vantagens`);
  if (attack.blastAvailable) parts.push(`Explosão disponível · dano ${attack.blastDamage}`);
  el.className='result success'; el.textContent=parts.join(' · ');
}

function initVehicleProfiles(){
  const select=$('vehicleProfileSelect'); if(!select) return; select.innerHTML='';
  for(const [key,p] of Object.entries(game.vehicleProfiles())){ const o=document.createElement('option'); o.value=key; o.textContent=`${p.shortName} · ${p.faction}`; select.append(o); }
}

function renderVehicle(state){
  if(!$('vehicleData')) return;
  const vehicle=game.activeVehicle(); $('vehicleNoActive').hidden=Boolean(vehicle); $('vehicleData').hidden=!vehicle;
  const swe02p=state.world?.publishedAdventure;
  const swe02Scene=swe02p?.id==='long-arm-of-the-hutt'&&swe02p.status==='ACTIVE';
  $('swe02NewMeenEscort').hidden=!(swe02Scene&&swe02p.currentNodeId==='new-meen-evacuation');
  $('swe02NewMeenMilitia').hidden=!(swe02Scene&&['new-meen-miners','new-meen-night-raid','drombb-cantina'].includes(swe02p.currentNodeId));
  const shipScene=swe02Scene&&['krayt-fang-logs','krayt-fang-ground-inspection'].includes(swe02p.currentNodeId);
  $('swe02KraytFangShip').hidden=!shipScene;
  if(shipScene){
    const flags=swe02p.flags||{},checks=state.master?.openingRequest?.adventureId==='long-arm-of-the-hutt'?state.master.openingRequest.checks||[]:[];
    const select=$('swe02ShipCheck'),previous=select.value;
    select.replaceChildren(new Option('Selecione um teste disponível',''),...checks.filter(x=>Number.isInteger(x.index)).map(x=>new Option(`${x.skill} ${x.difficultyText} · ${x.purpose}`,String(x.index))));
    if(checks.some(x=>String(x.index)===previous))select.value=previous;
    const seq=swe02p.shipSequence||{};
    $('swe02ShipStatus').textContent=[
      `Rota ${flags.shipCoursePlotted?(flags.shipCourseNonOptimal?'não ótima':'traçada'):'pendente'}`,
      `B’ura ${flags.shipBuraFound?'libertado':'não localizado'}`,
      `rastreador ${flags.shipTrackerSlowed?'reduzido':flags.shipTrackerLocated?'localizado':'sem diagnóstico'}`,
      `klaxon ${flags.shipKlaxonOff?'desligado':'ativo'}`,
      `música ${flags.shipMusicOff?'desligada':'ativa'}`,
      `${seq.discoveredRooms?.length||0} compartimentos examinados`,
      seq.transit?.completed?'viagem de 6 horas registrada':null
    ].filter(Boolean).join(' · ');
    $('swe02ShipFindBura').disabled=Boolean(flags.shipBuraFound)||swe02p.currentNodeId!=='krayt-fang-logs';
    $('swe02ShipSetPeltSearch').disabled=Boolean(flags.shipPeltsFound)||swe02p.currentNodeId!=='krayt-fang-logs';
    $('swe02ShipResolveCheck').disabled=!select.value||Boolean(state.combat?.active);
  }
  const militiaWeapons=state.player?.character?.weapons||[],weaponSelect=$('swe02MilitiaWeapon'),priorWeapon=weaponSelect.value;
  weaponSelect.replaceChildren(new Option('Arma blaster própria',''),...militiaWeapons.map((w,i)=>({w,i})).filter(({w})=>/blaster|pistola/i.test(String(w.name||''))&&Number(w.damage)>0).map(({w,i})=>new Option(w.name,String(i))));
  if([...weaponSelect.options].some(o=>o.value===priorWeapon))weaponSelect.value=priorWeapon;
  const militiaTarget=$('swe02MilitiaTarget'),oldTarget=militiaTarget.value;
  const militiaEnemies=state.combat?.active?(state.combat.adversaries||[]).filter(a=>a.profileKey==='laoth_drombb_thug'&&a.status==='active'):[];
  militiaTarget.replaceChildren(new Option('Grupo de capangas',''),...militiaEnemies.map(a=>new Option(a.name,a.id)));
  if(militiaEnemies.some(a=>a.id===oldTarget))militiaTarget.value=oldTarget;
  $('swe02MilitiaArm').disabled=Boolean(state.combat?.active)||!weaponSelect.value;
  $('swe02MilitiaRound').disabled=!militiaEnemies.length||!(swe02p?.newMeenMilitia?.members>0);
  const returnSelect=$('swe02MilitiaReturnLoan'),priorReturn=returnSelect.value;
  const ownerKey=String(state.player?.character?.id||state.player?.character?.name||'');
  const loans=swe02p?.newMeenMilitia?.loanedWeapons||[],legacyLosses=swe02p?.newMeenMilitia?.losses||0;
  const returnable=loans.filter((loan,i)=>loan.ownerKey===ownerKey&&(loan.status==='ACTIVE'||(!loan.status&&i>=legacyLosses)));
  returnSelect.replaceChildren(new Option('Arma com miliciano sobrevivente',''),...returnable.map(loan=>new Option(`${loan.weapon.name} · ${loan.loanId}`,loan.loanId)));
  if(returnable.some(x=>x.loanId===priorReturn))returnSelect.value=priorReturn;
  $('swe02MilitiaReturn').disabled=Boolean(state.combat?.active)||!returnSelect.value;


  const swe02Contracts=state.world?.publishedAdventure?.id==='long-arm-of-the-hutt'&&state.world.publishedAdventure.currentNodeId==='piddock-party';
  const swe02ContractsP=state.world?.publishedAdventure;
  $('swe02Act2Contracts').hidden=!swe02Contracts;
  $('swe02PiddockAsk').disabled=!swe02Contracts||Boolean(state.combat?.active)||Boolean(swe02ContractsP?.piddockConversationClosed);
  $('swe02PiddockTrade').disabled=!swe02Contracts||Boolean(state.combat?.active)||Boolean(swe02ContractsP?.piddockTradeInterest)||Boolean(swe02ContractsP?.piddockConversationClosed);
  $('swe02VioHire').disabled=!swe02Contracts||Boolean(state.combat?.active)||Boolean(swe02ContractsP?.vioHireReceipt);
  const piddockAgent=swe02ContractsP?.id==='long-arm-of-the-hutt'&&swe02ContractsP.currentNodeId==='piddock-freighter-handoff';
  $('swe02PiddockArmsPanel').hidden=!(swe02Contracts||piddockAgent);
  const armsCatalog=(swe02Contracts||piddockAgent)?game.swe02PiddockArmsCatalog():[];
  const piddockWeaponSelect=$('swe02PiddockWeapon'),previousWeapon=piddockWeaponSelect.value;
  piddockWeaponSelect.replaceChildren(new Option('Escolha uma arma',''),...armsCatalog.map(x=>new Option(`${x.name} · ${x.price} cr${x.agreed?' · acordada':''}${x.delivered?' · entregue':''}`,x.id)));
  if(armsCatalog.some(x=>x.id===previousWeapon))piddockWeaponSelect.value=previousWeapon;
  const selectedArm=armsCatalog.find(x=>x.id===piddockWeaponSelect.value);
  $('swe02PiddockAgree').disabled=!swe02Contracts||!swe02ContractsP?.piddockTradeInterest||Boolean(state.combat?.active)||!selectedArm||selectedArm.agreed;
  $('swe02PiddockDeliver').disabled=!piddockAgent||Boolean(state.combat?.active)||!selectedArm||!selectedArm.agreed||selectedArm.delivered;

  const swe02Gantry=swe02ContractsP?.id==='long-arm-of-the-hutt'&&swe02ContractsP.currentNodeId==='gantry-aa7'&&
    state.combat?.active&&state.combat?.publishedAdventureContext?.encounterId==='laoth-gantry-aa7';
  $('swe02Act2Gantry').hidden=!swe02Gantry;
  const gand=state.combat?.adversaries?.find(a=>a.profileKey==='laoth_vrixxtt_findsman'&&a.status==='active');
  $('swe02VrixxttBribe').disabled=!swe02Gantry||!gand||Boolean(swe02ContractsP?.vrixxttBribeReceipt)||Boolean(state.combat?.playerTurn?.actionUsed);
  const gantrySelect=$('swe02VrixxttTarget'),oldGantry=gantrySelect.value;
  const geonosians=swe02Gantry?state.combat.adversaries.filter(a=>a.profileKey==='laoth_geonosian_soldier'&&a.status==='active'):[];
  gantrySelect.replaceChildren(new Option('Sem soldado disponível',''),...geonosians.map(a=>new Option(a.name,a.id)));
  if(geonosians.some(a=>a.id===oldGantry))gantrySelect.value=oldGantry;
  const anatta=state.world?.publishedAdventure?.id==='long-arm-of-the-hutt'&&state.world.publishedAdventure.currentNodeId==='piddock-party';
  $('swe02Act2AnattaActions').hidden=!anatta;
  $('swe02AnattaBuy').disabled=!anatta||Boolean(state.combat?.active);
  if(anatta){const catalog=game.swe02AnattaCatalog(),select=$('swe02AnattaTopic'),selected=select.value;
    const bought=new Set((state.world.publishedAdventure.anattaTradeReceipts||[]).map(x=>x.topicId));
    select.replaceChildren(new Option('Selecione a informação',''),...Object.entries(catalog).filter(([id])=>!bought.has(id)).map(([id,x])=>new Option(`${id} · ${x.credits} créditos`,id)));
    if(catalog[selected]&&!bought.has(selected))select.value=selected;
  }
  const act3=state.world?.publishedAdventure?.id==='long-arm-of-the-hutt'&&
    (state.world.publishedAdventure.currentNodeId.startsWith('palace-')||
     ['teemo-showdown','tatooine-infiltration','mos-shuuta-electrogate'].includes(state.world.publishedAdventure.currentNodeId));
  $('swe02Act3Actions').hidden=!act3;
  $('swe02PalaceDeclarative').hidden=!act3;
  const palaceOperations=act3?game.swe02PalaceOperationCatalog():[];
  const opSelect=$('swe02PalaceOperationSelect'),priorOp=opSelect.value;
  opSelect.replaceChildren(new Option('Selecione a operação da sala',''),...palaceOperations.map(x=>new Option(`${x.label}${x.pendingRuleProfile?' · lacrado':''}`,x.id)));
  if(palaceOperations.some(x=>x.id===priorOp))opSelect.value=priorOp;
  $('swe02PalaceOperationCommit').disabled=!act3||!palaceOperations.length||Boolean(state.combat?.active);

  const dossier=state.world?.publishedAdventure?.act3Dossier;
  $('swe02Act3Dossier').disabled=!act3||Boolean(dossier)||Boolean(state.combat?.active);
  $('swe02Act3Resolve').disabled=!act3||!dossier||Boolean(state.world?.publishedAdventure?.act3Resolution)||Boolean(state.combat?.active);
  $('swe02Act3Chandelier').disabled=!act3||state.world?.publishedAdventure?.currentNodeId!=='teemo-showdown'||
    !state.combat?.active||state.combat?.publishedAdventureContext?.encounterId!=='teemo-palace-guards';
  $('swe02Act3Strongbox').disabled=!act3||state.world?.publishedAdventure?.currentNodeId!=='teemo-showdown'||
    Boolean(state.combat?.active)||state.world?.publishedAdventure?.flags?.teemoDefeated!==true||Boolean(state.world?.publishedAdventure?.act3Strongbox);

  if(!vehicle) return;
  const status=vehicleStatus(vehicle,{operator:state.player?.character}); const combat=state.vehicles?.combat ?? {};
  $('vehicleName').textContent=vehicle.shortName ?? vehicle.name; $('vehicleMeta').textContent=[vehicle.type,vehicle.faction,vehicle.hyperdrive?'hiperimpulsor':'sem hiperimpulsor'].filter(Boolean).join(' · ');
  $('vehicleSpeed').textContent=`${vehicle.speed}/${vehicle.speedMax}`; $('vehicleSilhouette').textContent=vehicle.silhouette; $('vehicleHandling').textContent=vehicle.handling>=0?`+${vehicle.handling}`:vehicle.handling; $('vehicleArmor').textContent=vehicle.armor;
  $('vehicleHull').textContent=`${vehicle.hullDamage}/${vehicle.hullThreshold}`; $('vehicleStrain').textContent=`${vehicle.systemStrain}/${vehicle.systemStrainThreshold}`; $('vehicleDefense').textContent=`${status.defense.proa}/${status.defense.popa}`; $('vehicleStatus').textContent=status.label;
  const ft=vehicle.roundEffects?.fullThrottle;
  const swe02Combat=combat.publishedVehicleContext?.id==='swe02-thwheek-p239';
  const swe02Crash=state.world?.publishedAdventure?.currentNodeId==='thwheek-crash';
  $('swe02VehicleActions').hidden=!(swe02Combat||swe02Crash);
  const nabatNode=state.world?.publishedAdventure?.id==='long-arm-of-the-hutt'&&
    ['nabat-arrival','nabat-contact','krayt-fang-ground-inspection'].includes(state.world?.publishedAdventure?.currentNodeId);
  $('swe02NabatActions').hidden=!nabatNode;
  const newMeenNode=state.world?.publishedAdventure?.id==='long-arm-of-the-hutt'&&['new-meen-miners','new-meen-night-raid','drombb-compound-approach','drombb-cantina'].includes(state.world?.publishedAdventure?.currentNodeId);
  $('swe02NewMeenSocial').hidden=!(state.world?.publishedAdventure?.id==='long-arm-of-the-hutt'&&(newMeenNode||state.world?.publishedAdventure?.currentNodeId==='drombb-cantina'));
  $('swe02NewMeenLifecycle').hidden=!newMeenNode;
  const newMeenCombat=state.combat?.active&&['laoth-new-meen-night-raid','laoth-drombb-cantina'].includes(state.combat?.publishedAdventureContext?.encounterId);
  const newMeenGroup=$('swe02NewMeenGroup'),oldGroup=newMeenGroup.value;
  const thugGroups=newMeenCombat?(state.combat.adversaries||[]).filter(x=>x.profileKey==='laoth_drombb_thug'):[];
  newMeenGroup.replaceChildren(new Option('Grupo de capangas em combate',''),...thugGroups.map(x=>new Option(`${x.name} · ${x.id}`,x.id)));
  if(thugGroups.some(x=>x.id===oldGroup))newMeenGroup.value=oldGroup;
  $('swe02NewMeenConfirmDeaths').disabled=!newMeenCombat;
  $('swe02NewMeenCoerce').disabled=!newMeenCombat||state.combat.publishedAdventureContext.encounterId!=='laoth-drombb-cantina';
  $('swe02DrombbSafeOpen').disabled=state.combat?.active||state.world?.publishedAdventure?.flags?.drombbThreatNeutralized!==true;
  const lifecycleStage=state.world?.publishedAdventure?.newMeenLifecycle?.stage;
  $('swe02NewMeenNightResult').disabled=Boolean(state.combat?.active)||lifecycleStage!=='NIGHT_COMBAT';
  if(nabatNode){
    const market=game.swe02NabatMarket();const select=$('swe02NabatItem');
    const selected=select.value;
    select.replaceChildren(new Option('Selecione um item do catálogo',''),...market.map(x=>new Option(`${x.name} — ${x.price} créditos`,x.id)));
    if(market.some(x=>x.id===selected))select.value=selected;
    const injuries=state.player?.character?.criticalInjuries||[];
    const criticalSelect=$('swe02NynCriticalId'),selectedInjury=criticalSelect.value;
    criticalSelect.replaceChildren(new Option('Selecione um Ferimento Crítico',''),...injuries.filter(x=>x.id).map(x=>new Option(`${x.label||x.code||x.id} · ${x.id}`,x.id)));
    if(injuries.some(x=>x.id===selectedInjury))criticalSelect.value=selectedInjury;
  }

  for(const id of ['swe02Resolve','swe02Pursue','swe02Linked'])$(''+id).disabled=!swe02Combat||!combat.active;
  for(const id of ['swe02CrashPilot','swe02CrashCrew'])$(''+id).disabled=!swe02Crash;
  $('vehicleCombatLine').textContent=combat.active?`Assalto ${combat.round} · alcance ${combat.range} · ação ${combat.actionUsed?'usada':'disponível'} · manobras ${combat.maneuversUsed}/2${ft?` · Aceleração máxima +${ft.bonus} até o assalto ${ft.expiresAfterRound}`:''}`:'Fora de combate.';
  $('startVehicleCombat').disabled=combat.active; $('nextVehicleRound').disabled=!combat.active; $('endVehicleCombat').disabled=!combat.active; $('vehicleAttack').disabled=!combat.active || combat.actionUsed || !status.weaponsOnline;
  const hasFullThrottle=game.talentRank('FULLTH')>0;
  const hasImprovedFullThrottle=game.talentRank('FULLTHIMP')>0;
  $('fullThrottleAction').disabled=!combat.active || combat.actionUsed || !hasFullThrottle;
  $('fullThrottleManeuver').disabled=!combat.active || combat.maneuversUsed>=2 || !hasImprovedFullThrottle;
  $('controlledFireAction').hidden=true; $('controlledFireAction').disabled=true;
  const weapons=$('vehicleWeaponSelect'); const prev=weapons.value; weapons.innerHTML=''; const combatRangeIndex=PLANETARY_RANGE_BANDS.indexOf(combat.range); let firstInRange=null; for(const [i,w] of vehicle.weapons.entries()){const o=document.createElement('option');o.value=i; const maxRangeIndex=PLANETARY_RANGE_BANDS.indexOf(w.range); const inRange=!combat.active || combatRangeIndex<=maxRangeIndex; o.textContent=`${w.name} · dano ${w.damage} · alcance ${w.range}${inRange?'':' · FORA DE ALCANCE'}`; o.disabled=!inRange; if(inRange&&firstInRange===null)firstInRange=String(i); weapons.append(o);} if([...weapons.options].some(o=>o.value===prev&&!o.disabled))weapons.value=prev; else if(firstInRange!==null)weapons.value=firstInRange; $('vehicleAttack').disabled=!combat.active || combat.actionUsed || !status.weaponsOnline || firstInRange===null;
}

function renderVehicleAttack(attack){
  const el=$('vehicleAttackResult'); if(!attack){el.className='result empty';el.textContent='Nenhum ataque veicular resolvido.';return;}
  if(!attack.hit){el.className='result fail';el.textContent=`Falha · dificuldade ${attack.difficulty}.`;return;}
  const parts=[`Acerto · dano ${attack.damage}`,`Blindagem efetiva ${attack.targetDamage?.effectiveArmor ?? '—'}`,`casco sofreu ${attack.targetDamage?.suffered ?? 0}`]; if(attack.criticalAvailable)parts.push('Impacto crítico disponível'); el.className='result success';el.textContent=parts.join(' · ');
}

function showFeedback(id, text) { $(id).textContent=text; }

$('sendIntent').addEventListener('click', async () => {
  try {
    const text=$('playerIntent').value;
    if (game.state.settings?.masterProvider==='http') {
      showFeedback('intentFeedback','Consultando o Mestre remoto...');
      const result=await game.requestMasterTurn({intentText:text,endpoint:game.state.settings.masterEndpoint});
      showFeedback('intentFeedback',result.proposal.askPlayer || 'O Mestre atualizou a cena.');
    } else {
      const result=game.submitIntent(text);
      showFeedback('intentFeedback',result.master.playerMessage);
    }
    $('playerIntent').value = '';
    render();
  } catch (e) { showFeedback('intentFeedback',e.message); }
});

$('consultRules').addEventListener('click',()=>{
  try {
    const result=game.consultRules($('ruleQuery').value);
    renderRuleResults(result);
    render();
  } catch(e) {
    $('ruleQueryStatus').textContent=e.message;
  }
});
$('ruleQuery').addEventListener('keydown',e=>{ if(e.key==='Enter'){ e.preventDefault(); $('consultRules').click(); } });

$('skillSelect').addEventListener('change',()=>renderExpertTalentOption(game.state.player.character));

$('rollCheck').addEventListener('click', () => {
  try {
    const result = game.resolveSkillCheck({
      skillKey: $('skillSelect').value,
      difficulty: $('difficulty').value,
      challenge: $('challenge').value,
      boost: $('boost').value,
      setback: $('setback').value,
      useExpertTalent: $('useExpertTalent')?.checked ?? false
    });
    renderRoll(result); render();
  } catch (e) {
    const el = $('rollResult'); el.className='result fail'; el.textContent=e.message;
  }
});

$('createCustomCharacter').addEventListener('click',()=>{
  try {
    const character=game.createCustomCharacter(creatorSelections());
    showFeedback('customCreationFeedback',`${character.name} criado e salvo. Restam ${character.xp.available} PE e ${character.credits} créditos.`);
    render();
  } catch(e) { showFeedback('customCreationFeedback',e.message); }
});

$('progressSkill').addEventListener('change',()=>renderProgression(game.state.player.character));
$('trainSkill').addEventListener('click',()=>{
  try {
    const result=game.trainSkill($('progressSkill').value);
    showFeedback('progressFeedback',`Perícia aumentada para grau ${result.newRank}; ${result.cost} PE gastos.`);
    render();
  } catch(e) { showFeedback('progressFeedback',e.message); }
});

$('startSession').addEventListener('click',()=>{
  try { const result=game.startSession(); showFeedback('sessionFeedback',`Sessão ${result.session.number} iniciada.`); render(); }
  catch(e) { showFeedback('sessionFeedback',e.message); }
});
$('closeSession').addEventListener('click',()=>{
  try { const result=game.closeSession(); showFeedback('sessionFeedback',`${result.session.summary} PE não foram concedidos automaticamente.`); render(); }
  catch(e) { showFeedback('sessionFeedback',e.message); }
});
$('resolveSuggestedCheck').addEventListener('click',()=>{
  try { const check=game.resolveSuggestedMasterCheck(); renderRoll(check); showFeedback('intentFeedback','Teste sugerido pelo Mestre resolvido e registrado.'); render(); }
  catch(e) { showFeedback('intentFeedback',e.message); }
});

for(const [id,run,okText] of [
 ['swe02LylekSlow',()=>game.chooseSWE02OldLylekRoute({slowDown:true}),'Velocidade reduzida; o terreno pode ser atravessado sem teste.'],
 ['swe02LylekFast',()=>game.chooseSWE02OldLylekRoute({slowDown:false}),'Rota rápida: resolva os dois testes de Pilotagem Média sugeridos, um por vez.'],
 ['swe02TrexHistorySave',()=>game.selectSWE02TrexCarryover({history:$('swe02TrexHistory').value}),'Histórico de T’rex registrado para a composição do combate (SWE02 p.15).'],
 ['swe02LylekProceed',()=>game.proceedSWE02OldLylekBluff(),'Aproximação pelo bluff registrada.'],
 ['swe02LylekFoot',()=>game.approachSWE02OldLylekOnFoot(),'Grupo parou e se aproxima a pé; combate pode começar.'],
 ['swe02LylekCrash',()=>game.resolveSWE02OldLylekCrashCheck({skillKey:$('swe02LylekCrashSkill').value}),'Teste de colisão registrado.'],
 ['swe02LylekHunters',()=>game.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:Math.random}),'Combate dos caçadores iniciado.'],
 ['swe02LylekInsist',()=>game.insistSWE02OldLylek(),'B’ura B’an advertiu para deixar o lylek com a presa.'],
 ['swe02LylekLoot',()=>game.inspectSWE02OldLylekSpeeder(),'Speeder inspecionado: encontrados dois stimpacks, um medpac e um kit emergencial de reparo (SWE02 p.18).'],
 ['swe02LylekBeast',()=>{game.persistSWE02OldLylek();return game.beginPublishedAdventureEncounter('lylek-den-lylek',{rng:Math.random});},'O lylek atacou usando Captive Rancor do Beginner Game p.47.'],
 ['swe02LylekFinish',()=>game.finishCombat({gmConfirmedSubmission:$('swe02LylekSubmission').checked}),'Combate encerrado; verifique o desfecho registrado.'],
 ['swe02LylekContinue',()=>game.advancePublishedAdventure('reach-new-meen'),'A tripulação seguiu para New Meen.']
])$(id).addEventListener('click',()=>{try{const r=run();if(r?.ok===false)throw Error(r.message);showFeedback('swe02OldLylekFeedback',okText);render();}
 catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});
$('swe02LylekRuse').addEventListener('click',()=>{try{const r=game.recordSWE02OldLylekRuse({identificationAsked:$('swe02IdentificationAsked').value==='true',oskaraPresent:$('swe02OskaraPresent').value==='true',oskaraRecognizes:$('swe02OskaraRecognizes').value==='true'});if(!r.ok)throw Error(r.message);showFeedback('swe02OldLylekFeedback','Hail, ausência de uniforme, pergunta de identificação e reconhecimento condicional registrados; ruse presumidamente falha sem teste de Bluff (SWE02 p.17).');render();}catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});
$('swe02LylekTactics').addEventListener('click',()=>{try{const r=game.chooseSWE02OldLylekTactics({approach:$('swe02LylekApproach').value,frontalOutcome:$('swe02LylekApproach').value==='frontal'?$('swe02LylekFrontalOutcome').value:'not_applicable',lylekBranch:$('swe02LylekBranch').value});if(!r.ok)throw Error(r.message);showFeedback('swe02OldLylekFeedback','Mapa tático, abordagem e decisão de GM persistidos com fonte pp.16–18.');render();}catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});
 $('swe02PcImportButton').addEventListener('click',()=>{try{const character=JSON.parse($('swe02PcImport').value);const r=game.registerPartyCharacter({character});if(!r.ok)throw Error(r.message);showFeedback('swe02OldLylekFeedback',`Ficha ${r.data.character.name} adicionada ao roster com ID ${r.data.id}.`);render();}catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});
$('swe02CrewConfirm').addEventListener('click',()=>{try{const crewPcIds=[...document.querySelectorAll('[data-swe02-pc-id]:checked')].map(x=>x.dataset.swe02PcId);const r=game.chooseSWE02OldLylekCrew({crewPcIds});if(!r.ok)throw Error(r.message);showFeedback('swe02OldLylekFeedback','IDs de PCs a bordo registrados em SWE02 p.15.');render();}catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});
$('swe02LylekCheck').addEventListener('click',()=>{try{
 const raw=$('swe02RepairReduction').value;const r=game.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',repairReductionMinutes:raw===''?null:Number(raw),rng:Math.random});
 showFeedback('swe02OldLylekFeedback',`Teste ${r.check.success?'bem-sucedido':'falhou'} · ${r.adjudication?.source?.printedPage?'fonte p.'+r.adjudication.source.printedPage:'SWE02'}.`);render();
}catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});

for(const [id,run,label] of [
 ['swe02ShipObserveAlerts',()=>game.recordSWE02ShipObservation('observe-alert-icons'),'Quatro alertas registrados com estado persistente.'],
 ['swe02ShipLocateSmell',()=>game.recordSWE02ShipObservation('notice-smell-source-in-main-hold'),'Origem do odor registrada; Percepção para localizar a fonte não recebe Setback.'],
 ['swe02ShipInspectCage',()=>game.recordSWE02ShipObservation('press-cage-icon',{oskaraPresent:$('swe02ShipOskara').value==='true'}),'Ícone da cela consultado; B’ura identificado.'],
 ['swe02ShipSetPeltSearch',()=>game.setSWE02PeltSearchAnnounced($('swe02ShipAnnouncePeltSearch').checked),'Anúncio da busca salvo; o teste de Percepção recebe 1 Impulso se marcado.'],
 ['swe02ShipDiscoverMainHold',()=>game.discoverSWE02ShipRoom('main-cargo-hold'),'Conteúdo do porão principal descoberto; nada foi transferido ao inventário.'],
 ['swe02ShipDiscoverHoldTwo',()=>game.discoverSWE02ShipRoom('number-two-hold'),'Conteúdo do porão dois descoberto; nada foi transferido ao inventário.'],
 ['swe02ShipDiscoverHoldThree',()=>game.discoverSWE02ShipRoom('number-three-hold'),'Conteúdo do porão três descoberto; nada foi transferido ao inventário.'],
 ['swe02ShipLowhhrickStrain',()=>game.recordSWE02ShipObservation('lowhhrick-pelt-strain',{applyToActiveLowhhrick:$('swe02ShipApplyLowhhrickStrain').checked}),'Consequência de Lowhhrick registrada; Tensão aplicada somente à ficha ativa de Lowhhrick quando confirmado.']
])$(id).addEventListener('click',()=>{try{const r=run();if(r?.ok===false)throw Error(r.message);showFeedback('swe02ShipFeedback',label);render();}catch(e){showFeedback('swe02ShipFeedback',e.message);}});
$('swe02ShipFindBura').addEventListener('click',()=>{try{const r=game.advancePublishedAdventure('ship-find-bura');if(r?.ok===false)throw Error(r.message);showFeedback('swe02ShipFeedback','B’ura B’an encontrado e libertado sem teste.');render();}catch(e){showFeedback('swe02ShipFeedback',e.message);}});
$('swe02BuraConversation').addEventListener('click',()=>{try{
 const topics=[...$('swe02BuraTopics').selectedOptions].map(o=>o.value);
 const r=game.recordSWE02BuraConversation({topics,oskaraPresent:$('swe02BuraOskaraPresent').checked,oskaraSharedHistory:$('swe02BuraOskaraStory').checked,oskaraRaisedCorruption:$('swe02BuraCorruption').checked});
 if(r?.ok===false)throw Error(r.message);const data=r?.data?.facts||[];showFeedback('swe02ShipFeedback',`Conversa registrada: ${data.map(x=>x.fact).join(' ')}`);render();
}catch(e){showFeedback('swe02ShipFeedback',e.message);}});
$('swe02ShipResolveCheck').addEventListener('click',()=>{try{
 const r=game.resolvePublishedAdventureCheck(Number($('swe02ShipCheck').value),{adventureId:'long-arm-of-the-hutt',aidingLowhhrick:$('swe02ShipAidLowhhrick').checked,rng:Math.random});
 if(r?.ok===false)throw Error(r.message);const data=r?.data||r;renderRoll(data.check);showFeedback('swe02ShipFeedback',`Teste ${data.check.success?'bem-sucedido':'falhou'} · ${data.adjudication?.source?.printedPage?'SWE02 p.'+data.adjudication.source.printedPage:'SWE02'}${data.check.boost?.length?' · Impulso aplicado':''}.`);render();
}catch(e){showFeedback('swe02ShipFeedback',e.message);}});

$('loadStarter').addEventListener('click',()=>{
  try { game.loadStarterCharacter($('starterCharacter').value); render(); }
  catch(e){ alert(e.message); }
});

$('exportCharacter').addEventListener('click',()=>{
  try {
    $('characterJson').value=game.exportCharacter();
    showFeedback('characterJsonFeedback','Ficha exportada para o campo JSON.');
  } catch(e){ showFeedback('characterJsonFeedback',e.message); }
});

$('importCharacter').addEventListener('click',()=>{
  try {
    game.importCharacter($('characterJson').value);
    showFeedback('characterJsonFeedback','Ficha validada, importada e salva.');
    render();
  } catch(e){ showFeedback('characterJsonFeedback',`Importação recusada: ${e.message}`); }
});

$('startEncounter').addEventListener('click',()=>{
  try {
    const result=game.beginPresetEncounter({playerQuickReactionStrain:Number($('quickReactionStrain').value||0)});
    const first=currentSlot(result.combat)?.side==='player' ? 'Sua posição chegou primeiro.' : 'PNPCs agiram antes de você.';
    const qr=result.quickReaction?.spend?` Reação rápida: ${result.quickReaction.spend} de Tensão por +${result.quickReaction.boost} Impulso.`:'';
    showFeedback('encounterFeedback',`Encontro iniciado. ${first}${qr}`);
    render();
  } catch(e){ showFeedback('encounterFeedback',e.message); }
});
$('startPendingEncounter').addEventListener('click',()=>{
  try {
    const result=game.beginPendingMasterEncounter({playerQuickReactionStrain:Number($('quickReactionStrain').value||0)});
    const first=currentSlot(result.combat)?.side==='player' ? 'Sua posição chegou primeiro.' : 'PNPCs agiram antes de você.';
    showFeedback('encounterFeedback',`Encontro do Mestre iniciado. ${first}`);
    render();
  } catch(e){ showFeedback('encounterFeedback',e.message); }
});
$('startCombat').addEventListener('click',()=>{ try { game.beginCombat(); showFeedback('maneuverFeedback','Combate livre iniciado, sem adversários automáticos.'); render(); } catch(e){ showFeedback('maneuverFeedback',e.message); } });
$('nextTurn').addEventListener('click',()=>{
  try {
    const result=game.advancePlayerTurn();
    const turns=result?.npcTurns?.map(t=>t.text).join(' ') ?? '';
    showFeedback('maneuverFeedback',turns ? `Seu turno terminou. ${turns}` : 'Novo turno iniciado.');
    render();
  } catch(e){ showFeedback('maneuverFeedback',e.message); }
});
$('endCombat').addEventListener('click',()=>{ try { const result=game.finishCombat(); const n=result.recovery?.recovered ?? 0; showFeedback('maneuverFeedback',`Combate encerrado. Recuperou ${n} de Tensão ao fim do encontro.`); render(); } catch(e){ showFeedback('maneuverFeedback',e.message); } });
$('normalManeuver').addEventListener('click',()=>{ try { game.useManeuver({method:'normal'}); showFeedback('maneuverFeedback','Manobra registrada.'); render(); } catch(e){ showFeedback('maneuverFeedback',e.message); } });
$('strainManeuver').addEventListener('click',()=>{ try { game.useManeuver({method:'strain'}); showFeedback('maneuverFeedback','Segunda manobra registrada; +2 de Tensão.'); render(); } catch(e){ showFeedback('maneuverFeedback',e.message); } });
$('actionManeuver').addEventListener('click',()=>{ try { game.useManeuver({method:'action'}); showFeedback('maneuverFeedback','A ação foi trocada pela segunda manobra.'); render(); } catch(e){ showFeedback('maneuverFeedback',e.message); } });

$('weaponSelect').addEventListener('change',()=>renderAttackRangeControls(game.state.player.character));
$('targetRange').addEventListener('change',()=>renderAttackRangeControls(game.state.player.character));
$('targetAdversary').addEventListener('change',()=>renderAttackRangeControls(game.state.player.character));
$('targetAdversary').addEventListener('change',()=>renderSWE02OldLylek(game.state));
$('targetAdversary').addEventListener('change',()=>{$('swe02TargetBehindRocks').checked=false;});
$('swe02PlayerBehindRocks').addEventListener('change',()=>{try{const r=game.setSWE02OldLylekPlayerCover({behindRockPile:$('swe02PlayerBehindRocks').checked});if(!r.ok)throw Error(r.message);showFeedback('swe02OldLylekFeedback','Cobertura do PC registrada; ataques à distância dos caçadores recebem +2 Setback (SWE02 p.17).');render();}catch(e){showFeedback('swe02OldLylekFeedback',e.message);render();}});
$('swe02LylekRetreat').addEventListener('click',()=>{try{const r=game.resolveSWE02OldLylekHunterRetreat();if(!r.ok)throw Error(r.message);showFeedback('swe02OldLylekFeedback','Caçadores recuaram pelo caminho pontilhado da rota frontal (SWE02 p.16–17).');render();}catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});
$('swe02ArrivalDetails').addEventListener('click',()=>{try{const r=game.revealSWE02OldLylekDeveloperDetails({asked:true});if(!r.ok)throw Error(r.message);showFeedback('swe02OldLylekFeedback','Detalhes do desenvolvedor revelados após pergunta dos PCs (SWE02 pp.18–19).');render();}catch(e){showFeedback('swe02OldLylekFeedback',e.message);}});
$('aimButton').addEventListener('click',()=>{
  try { game.aim({maneuverMethod:$('aimManeuverMethod').value}); showFeedback('maneuverFeedback','Mirar preparado: +1 Impulso no próximo ataque.'); render(); }
  catch(e){ showFeedback('maneuverFeedback',e.message); }
});

$('attackButton').addEventListener('click',()=>{
  try {
    const attack=game.resolveWeaponAttack({
      weaponIndex:$('weaponSelect').value,
      targetAdversaryId:game.state.combat.encounter ? $('targetAdversary').value : null,
      targetRange:$('targetRange').value,
      targetBehindRockPile:$('swe02TargetBehindRocks').checked,
      challenge:$('attackChallenge').value,
      boost:$('attackBoost').value,
      setback:$('attackSetback').value
    });
    renderAttack(attack); render();
  } catch(e){ const el=$('attackResult'); el.className='result fail'; el.textContent=e.message; }
});

$('activateCritical').addEventListener('click',()=>{
  try {
    const result=game.activatePendingPlayerCritical();
    showFeedback('maneuverFeedback',result.woundsAdded>0 ? `Crítico ativado: o grupo perdeu um lacaio.` : 'Ferimento Crítico ativado no adversário.');
    render();
  } catch(e){ showFeedback('maneuverFeedback',e.message); }
});

$('applyDamage').addEventListener('click',()=>{
  try {
    const result=game.applyPlayerDamage({damage:$('incomingDamage').value,pierce:$('incomingPierce').value,damageType:$('incomingDamageType').value});
    showFeedback('damageFeedback',`Proteção efetiva ${result.effectiveSoak}; sofreu ${result.suffered} ${result.damageType==='strain'?'de Tensão':'Ferimentos'}${result.criticalAdded?' e recebeu 1 ferimento crítico':''}. Estado: ${result.status.label}.`);
    render();
  } catch(e){ showFeedback('damageFeedback',e.message); }
});

$('useStimpack').addEventListener('click',()=>{
  try {
    const result=game.usePlayerStimpack({maneuverMethod:$('stimpackManeuverMethod').value});
    showFeedback('stimpackFeedback',`Recuperou ${result.healed} Ferimento${result.healed===1?'':'s'}; restam ${result.remaining} injetor${result.remaining===1?'':'es'}.`);
    render();
  } catch(e){ showFeedback('stimpackFeedback',e.message); }
});

$('medicineButton').addEventListener('click',()=>{
  try {
    const result=game.useMedicine({medicalSupplies:$('medicineSupplies').value==='yes'});
    const outcome=result.check.success ? `Sucesso: recuperou ${result.healed} Ferimento(s) e ${result.strainRecovered} de Tensão.` : 'Falha no tratamento; nenhum Ferimento foi recuperado.';
    showFeedback('medicineFeedback',`${outcome} Dificuldade ${result.difficulty}.`);
    render();
  } catch(e){ showFeedback('medicineFeedback',e.message); }
});

$('restNightButton').addEventListener('click',()=>{
  try {
    const result=game.restFullNight();
    showFeedback('restFeedback',`Descanso concluído: recuperou ${result.woundsRecovered} Ferimento(s) e ${result.strainRecovered} de Tensão.`);
    render();
  } catch(e){ showFeedback('restFeedback',e.message); }
});


$('loadVehicle').addEventListener('click',()=>{ try{ const v=game.addVehicle($('vehicleProfileSelect').value); showFeedback('vehicleFeedback',`${v.shortName ?? v.name} adicionada à campanha.`); render(); }catch(e){showFeedback('vehicleFeedback',e.message);} });
$('startVehicleCombat').addEventListener('click',()=>{ try{ game.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Curto'}); showFeedback('vehicleManeuverFeedback','Encontro veicular iniciado contra um TIE/ln.'); render(); }catch(e){showFeedback('vehicleManeuverFeedback',e.message);} });
$('nextVehicleRound').addEventListener('click',()=>{ try{ game.nextVehicleRound(); showFeedback('vehicleManeuverFeedback','Novo assalto veicular.'); render(); }catch(e){showFeedback('vehicleManeuverFeedback',e.message);} });
$('endVehicleCombat').addEventListener('click',()=>{ try{ game.endVehicleCombat(); showFeedback('vehicleManeuverFeedback','Combate veicular encerrado.'); render(); }catch(e){showFeedback('vehicleManeuverFeedback',e.message);} });
$('vehicleView').addEventListener('click',e=>{});
for(const button of document.querySelectorAll('[data-vehicle-maneuver]')) button.addEventListener('click',()=>{ try{ const result=game.useVehiclePilotManeuver(button.dataset.vehicleManeuver,{method:$('vehicleManeuverMethod').value,direction:button.dataset.direction ?? 'closer'}); showFeedback('vehicleManeuverFeedback',`Manobra registrada. Velocidade ${result.vehicle.speed}; alcance ${game.state.vehicles.combat.range}.`); render(); }catch(e){showFeedback('vehicleManeuverFeedback',e.message);} });
$('fullThrottleAction').addEventListener('click',()=>{ try { const r=game.useFullThrottle({asManeuver:false}); showFeedback('vehicleManeuverFeedback',r.check.success?`Aceleração máxima: Velocidade máxima +${r.bonus} por ${r.durationRounds} assalto(s).`:'Aceleração máxima falhou.'); render(); } catch(e){ showFeedback('vehicleManeuverFeedback',e.message); } });
$('fullThrottleManeuver').addEventListener('click',()=>{ try { const r=game.useFullThrottle({asManeuver:true,secondManeuverMethod:$('vehicleManeuverMethod').value}); showFeedback('vehicleManeuverFeedback',r.check.success?`Aceleração máxima aprimorada: Velocidade máxima +${r.bonus} por ${r.durationRounds} assalto(s); +${r.personalStrainCost} Tensão pessoal.`:`Aceleração máxima aprimorada falhou; +${r.personalStrainCost} Tensão pessoal.`); render(); } catch(e){ showFeedback('vehicleManeuverFeedback',e.message); } });
$('controlledFireAction').addEventListener('click',()=>{ showFeedback('vehicleManeuverFeedback','Fogo controlado pertence ao legado Age of Rebellion e não está ativo no perfil EotE Core.'); });

for(const [id,method,args] of [
 ['swe02Resolve','resolveSWE02ThwheekCombat',{}],['swe02Pursue','resolveSWE02ThwheekCombat',{pursue:true}],
 ['swe02Linked','activateSWE02ThwheekLinked',null],['swe02CrashPilot','resolveSWE02CrashPilot',null],
 ['swe02CrashCrew','resolveSWE02CrashResilience',null]])
 $(''+id).addEventListener('click',()=>{try{const result=args===null?game[method]():game[method](args);
 showFeedback('vehicleManeuverFeedback',JSON.stringify(result?.outcome||result?.completed||result?.check?.success||'registrado'));render();}
 catch(e){showFeedback('vehicleManeuverFeedback',e.message)}});
for(const [id,method] of [
 ['swe02NabatWatch','requestSWE02SpaceportWatch'],['swe02NabatAlert','receiveSWE02SpaceportAlert'],
 ['swe02NabatRepair','repairSWE02KraytFangAtNabat'],['swe02NabatCare','treatSWE02NabatWounds']])
 $(id).addEventListener('click',()=>{try{const result=game[method]();showFeedback('swe02NabatFeedback',`Registrado: ${result.type||'SWE02'}`);render();}
 catch(e){showFeedback('swe02NabatFeedback',e.message);}});
$('swe02NynCritical').addEventListener('click',()=>{try{
 const r=game.treatSWE02NynCritical({injuryId:$('swe02NynCriticalId').value.trim()});
 showFeedback('swe02NabatFeedback',r.healed?'Ferimento Crítico tratado.':'Tentativa médica falhou; não é possível repetir para esta lesão.');render();
}catch(e){showFeedback('swe02NabatFeedback',e.message);}});
$('swe02NynNegotiate').addEventListener('click',()=>{try{
 const r=game.negotiateSWE02Nyn({method:$('swe02NynMethod').value});
 showFeedback('swe02NabatFeedback',`Nyn: ${r.check.success?'sucesso':'falha'} · ${r.check.netAdvantage} Vantagens · ${r.check.triumph} Triunfos. Armas pendentes de perfil EotE.`);render();
}catch(e){showFeedback('swe02NabatFeedback',e.message);}});
for(const [id,method,args] of [
 ['swe02EscortPrepare','prepareSWE02NewMeenEscort',null],
 ['swe02EscortDepart','departSWE02NewMeenEscort',()=>({travelTicks:Number($('swe02EscortTicks').value)})],
 ['swe02EscortArrive','arriveSWE02NewMeenEscort',null],
 ['swe02EscortAbandon','abandonSWE02NewMeenEscort',null]])
 $(id).addEventListener('click',()=>{try{const r=args===null?game[method]():game[method](args());
 showFeedback('swe02EscortFeedback',`Escolta: ${r.type||r.receipt?.type||'registrada'} · sem vitória automática em New Meen.`);render();}
 catch(e){showFeedback('swe02EscortFeedback',e.message);}});
$('swe02MilitiaArm').addEventListener('click',()=>{try{
 const r=game.armSWE02NewMeenMilitia({weaponIndex:Number($('swe02MilitiaWeapon').value)});
 showFeedback('swe02MilitiaFeedback',`Arma transferida: ${r.weaponName}. O mineiro está armado.`);render();
}catch(e){showFeedback('swe02MilitiaFeedback',e.message);}});
$('swe02MilitiaReturn').addEventListener('click',()=>{try{
 const r=game.returnSWE02NewMeenMilitiaWeapon({loanId:$('swe02MilitiaReturnLoan').value});
 showFeedback('swe02MilitiaFeedback',`Arma devolvida ao proprietário: ${r.weaponName}. Nenhuma arma de aliado eliminado é recuperada automaticamente.`);render();
}catch(e){showFeedback('swe02MilitiaFeedback',e.message);}});
$('swe02MilitiaRound').addEventListener('click',()=>{try{
 const r=game.applySWE02NewMeenMilitiaRound({targetGroupId:$('swe02MilitiaTarget').value});
 showFeedback('swe02MilitiaFeedback',`Apoio aprovado: ${r.woundsInflicted} Ferimento(s) no inimigo; 1 miliciano perdido.`);render();
}catch(e){showFeedback('swe02MilitiaFeedback',e.message);}});
$('swe02NewMeenBribe').addEventListener('click',()=>{try{
 const r=game.bribeSWE02NewMeenThug({credits:Number($('swe02NewMeenCredits').value)});
 showFeedback('swe02NewMeenFeedback',r.receipt.success?`Capanga deixou o grupo; ${r.receipt.creditsSpent} créditos debitados. Restam ${r.remainingThugs}.`:'Negociação falhou; nenhum crédito debitado.');render();
}catch(e){showFeedback('swe02NewMeenFeedback',e.message);}});
const recordNewMeen=(operation,details={})=>{const r=game.recordSWE02NewMeenLifecycle(operation,details);if(r?.ok===false)throw Error(r.error?.message||r.message||'Falha ao registrar decisão New Meen.');return r;};
$('swe02NewMeenNightResolve').addEventListener('click',()=>{try{
 const choice=$('swe02NewMeenNightChoice').value,details={choice};if(choice==='other')details.gmDisposition=$('swe02NewMeenGmNote').value.trim();
 const r=recordNewMeen('night_stay',details);showFeedback('swe02NewMeenLifecycleFeedback',`Escolha registrada · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenNightInteraction').addEventListener('click',()=>{try{
 const response=$('swe02NewMeenNightResponse').value,details={response};if(response==='hand_over_speeder')details.gmDisposition=$('swe02NewMeenGmNote').value.trim();
 const r=recordNewMeen('night_interaction',details);showFeedback('swe02NewMeenLifecycleFeedback',`Resposta registrada · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenNightResult').addEventListener('click',()=>{try{
 const outcome=$('swe02NewMeenNightOutcome').value,details={outcome,gmDisposition:$('swe02NewMeenGmNote').value.trim()};
 const r=recordNewMeen('night_combat',details);showFeedback('swe02NewMeenLifecycleFeedback',`Resultado do combate noturno registrado · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenEscortRefuse').addEventListener('click',()=>{try{
 const r=recordNewMeen('refuse_escort');showFeedback('swe02NewMeenLifecycleFeedback',`Recusa registrada; os capangas se voltam aos PJs · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenCoercionResolve').addEventListener('click',()=>{try{
 const coerced=$('swe02NewMeenCoercionSucceeded').checked,details={coerced};if(!coerced)details.gmRuling=$('swe02NewMeenGmNote').value.trim();
 const r=recordNewMeen('coercion',details);showFeedback('swe02NewMeenLifecycleFeedback',`Coerção registrada · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenRoadHarassment').addEventListener('click',()=>{try{
 const r=recordNewMeen('road_harassment',{gmDisposition:$('swe02NewMeenGmNote').value.trim()});showFeedback('swe02NewMeenLifecycleFeedback',`Adjudicação da estrada registrada · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenRetaliationResolve').addEventListener('click',()=>{try{
 const choice=$('swe02NewMeenRetaliation').value,details={choice};if(choice!=='go_to_compound')details.gmDisposition=$('swe02NewMeenGmNote').value.trim();
 const r=recordNewMeen('retaliation',details);showFeedback('swe02NewMeenLifecycleFeedback',`Retaliação registrada · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenApproach').addEventListener('click',()=>{try{
 const r=recordNewMeen('compound_approach',{precautions:$('swe02NewMeenPrecautions').checked});showFeedback('swe02NewMeenLifecycleFeedback',`Aproximação registrada · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenDetectedResolve').addEventListener('click',()=>{try{
 const response=$('swe02NewMeenDetected').value,details={response};if(response!=='negotiate_politely')details.gmDisposition=$('swe02NewMeenGmNote').value.trim();
 const r=recordNewMeen('detected_contact',details);showFeedback('swe02NewMeenLifecycleFeedback',`Contato registrado · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenAllyResolve').addEventListener('click',()=>{try{
 const r=recordNewMeen('ally_resolution',{method:$('swe02NewMeenAllyMethod').value});showFeedback('swe02NewMeenLifecycleFeedback',`Método de resolução dos aliados registrado · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenCharm').addEventListener('click',()=>{try{
 const r=recordNewMeen('charm_attempt');showFeedback('swe02NewMeenLifecycleFeedback',`Tentativa de Charme registrada sem rolagem · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenDefeatResolve').addEventListener('click',()=>{try{
 const r=recordNewMeen('cantina_outcome',{outcome:'pcs_defeated',defeatResolution:$('swe02NewMeenDefeat').value});showFeedback('swe02NewMeenLifecycleFeedback',`Consequência da derrota real registrada · ${r.lifecycle.stage}.`);render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenOfficeSearch').addEventListener('click',()=>{try{
 const r=recordNewMeen('office_search');showFeedback('swe02NewMeenLifecycleFeedback',r.lifecycle.messages.found?'Mensagens oficiais registradas como encontradas.':'Teste Hard Computers falhou; nenhuma informação foi descoberta.');render();
}catch(e){showFeedback('swe02NewMeenLifecycleFeedback',e.message);}});
$('swe02NewMeenConfirmDeaths').addEventListener('click',()=>{try{
 const r=game.adjudicateSWE02NewMeenDeaths({groupId:$('swe02NewMeenGroup').value,confirmedDeaths:Number($('swe02NewMeenDeaths').value)});
 showFeedback('swe02NewMeenFeedback',`Mestre confirmou ${r.confirmedDeaths} morte(s), limitadas a ${r.verifiedCasualties} baixa(s) verificadas.`);render();
}catch(e){showFeedback('swe02NewMeenFeedback',e.message);}});
$('swe02NewMeenCoerce').addEventListener('click',()=>{try{
 const r=game.coerceSWE02NewMeenThugs();showFeedback('swe02NewMeenFeedback',r.success?`Coerção bem-sucedida: ${r.surrenderedGroupIds.length} grupo(s) se renderam; Drombb continua hostil.`:`Coerção falhou (dificuldade ${r.difficulty}). Ação consumida.`);render();
}catch(e){showFeedback('swe02NewMeenFeedback',e.message);}});
$('swe02DrombbSafeOpen').addEventListener('click',()=>{try{
 const r=game.openSWE02DrombbSafe({method:$('swe02DrombbSafeMethod').value,gmConfirmedQuick:$('swe02DrombbSafeQuick').checked});
 showFeedback('swe02NewMeenFeedback',r.creditsGranted?`Cofre aberto: ${r.creditsGranted} créditos adicionados à carteira real.`:r.success?'Cofre encontrado, mas os Twi’leks chegaram primeiro; nenhum crédito concedido.':'Cofre fechado: teste falhou, nenhum crédito concedido.');render();
}catch(e){showFeedback('swe02NewMeenFeedback',e.message);}});
$('swe02PiddockAsk').addEventListener('click',()=>{try{
 const r=game.askSWE02Piddock({topic:$('swe02PiddockQuestion').value});
 showFeedback('swe02ContractsFeedback',`Pergunta ${r.ordinal} registrada${r.conversationClosed?' — Piddock encerrou a conversa':r.warning?' — Piddock exige negócios':''}.`);render();
}catch(e){showFeedback('swe02ContractsFeedback',e.message);}});
$('swe02PiddockTrade').addEventListener('click',()=>{try{
 game.showSWE02PiddockTradeInterest();showFeedback('swe02ContractsFeedback','Interesse comercial declarado, sem comprar armas automaticamente.');render();
}catch(e){showFeedback('swe02ContractsFeedback',e.message);}});
$('swe02PiddockAgree').addEventListener('click',()=>{try{
 const r=game.agreeSWE02PiddockArms({weaponId:$('swe02PiddockWeapon').value,quantity:Number($('swe02PiddockQuantity').value),haggle:$('swe02PiddockHaggle').checked});
 showFeedback('swe02ContractsFeedback',`Acordo registrado: ${r.quantity} × ${r.weaponId} por ${r.creditsDue} créditos. Pague ao agente no cargueiro.`);render();
 }catch(e){showFeedback('swe02ContractsFeedback',e.message);}});
$('swe02PiddockDeliver').addEventListener('click',()=>{try{
 const r=game.deliverSWE02PiddockArms({weaponId:$('swe02PiddockWeapon').value});
 showFeedback('swe02ContractsFeedback',`${r.quantity} arma(s) recebida(s); ${r.creditsSpent} créditos debitados.`);render();
 }catch(e){showFeedback('swe02ContractsFeedback',e.message);}});
$('swe02VioHire').addEventListener('click',()=>{try{
 const r=game.hireSWE02Vio({offer:$('swe02VioOffer').value});
 showFeedback('swe02ContractsFeedback',`Vio contratados para ${r.partySize} passageiro(s); ${r.creditsSpent} créditos debitados. Escolha a rota de embarque.`);render();
}catch(e){showFeedback('swe02ContractsFeedback',e.message);}});
$('swe02VrixxttBribe').addEventListener('click',()=>{try{
 const r=game.bribeSWE02Vrixxtt({credits:Number($('swe02VrixxttCredits').value),targetId:$('swe02VrixxttTarget').value||null});
 showFeedback('swe02GantryFeedback',`Vrixx’tt recebeu ${r.creditsSpent} créditos e se retirou. ${r.shot?`Tiro ${r.shot.check.success?'acertou':'falhou'}, ${r.shot.suffered} Ferimentos.`:'Sem tiro.'}`);render();
}catch(e){showFeedback('swe02GantryFeedback',e.message);}});
$('swe02AnattaBuy').addEventListener('click',()=>{try{
 const r=game.buySWE02AnattaInformation({topicId:$('swe02AnattaTopic').value});
 showFeedback('swe02AnattaFeedback',`${r.fact} · ${r.creditsSpent} créditos debitados.`);render();
}catch(e){showFeedback('swe02AnattaFeedback',e.message);}});
$('swe02GladiatorRecruit').addEventListener('click',()=>{try{
 const r=game.recruitSWE02Gladiators({method:$('swe02GladiatorMethod').value});
 showFeedback('swe02Act3Feedback',r.success?`Quatro gladiadores aliados; ${r.creditsSpent} créditos debitados.`:'Coerção falhou: gladiadores não se aliaram.');render();
}catch(e){showFeedback('swe02Act3Feedback',e.message);}});
$('swe02ThroneAlarmQueue').addEventListener('click',()=>{try{
 const r=game.queueSWE02ThroneAlarm({confirmedBarracksCount:Number($('swe02ThroneReinforcementCount').value)});
 showFeedback('swe02Act3Feedback',`Alarme autorizado para a próxima manobra real de Teemo. Guardas confirmados: ${r.confirmedBarracksCount}.`);render();
}catch(e){showFeedback('swe02Act3Feedback',e.message);}});
$('swe02PalaceOperationCommit').addEventListener('click',()=>{try{
 const id=$('swe02PalaceOperationSelect').value;
 const receipt=game.executeSWE02PalaceOperation({operationId:id});
 showFeedback('swe02PalaceOperationFeedback',receipt.usable?'Equipamento certificado transferido ao inventário.':receipt.mode==='CARGO'?'Objeto volumoso colocado no inventário; 450 créditos são apenas estimativa de revenda, não dinheiro recebido.':receipt.sealedAsset?'Reserva lacrada: nenhum efeito mecânico concedido.':'Informação registrada com fonte oficial.');
 render();
}catch(e){showFeedback('swe02PalaceOperationFeedback',e.message);}});
$('swe02Act3Dossier').addEventListener('click',()=>{try{
 const r=game.prepareSWE02AnattaDossier();showFeedback('swe02Act3Feedback',`Dossiê verificado: ${r.evidence.join(', ')}.`);render();
}catch(e){showFeedback('swe02Act3Feedback',e.message);}});
$('swe02Act3Resolve').addEventListener('click',()=>{try{
 const r=game.resolveSWE02TeemoWithoutCombat({method:$('swe02Act3Method').value});
 showFeedback('swe02Act3Feedback',r.method==='JABBA'?'Jabba recebeu as provas: Teemo removido, sem declarar morte.':'Teemo aceitou deixar o grupo em paz; permanece vivo.');render();
}catch(e){showFeedback('swe02Act3Feedback',e.message);}});
$('swe02Act3Chandelier').addEventListener('click',()=>{try{
 const r=game.shootSWE02TeemoChandelier({weaponIndex:Number($('weaponSelect').value)});
 showFeedback('swe02Act3Feedback',r.success?'Corrente atingida: Teemo caiu e os guardas fogem. Encerre o combate real.':'Tiro falhou; ação do turno consumida.');render();
}catch(e){showFeedback('swe02Act3Feedback',e.message);}});
$('swe02Act3Strongbox').addEventListener('click',()=>{try{
 const r=game.claimSWE02TeemoStrongbox();showFeedback('swe02Act3Feedback',`Cofre recolhido uma vez: ${r.credits} créditos reais.`);render();
}catch(e){showFeedback('swe02Act3Feedback',e.message);}});
$('swe02NabatBuy').addEventListener('click',()=>{try{
 const result=game.purchaseSWE02NabatItem({itemId:$('swe02NabatItem').value,quantity:Number($('swe02NabatQuantity').value)});
 showFeedback('swe02NabatFeedback',`Compra registrada: ${result.item.name} · ${result.receipt.cost} créditos.`);render();
}catch(e){showFeedback('swe02NabatFeedback',e.message);}});
$('vehicleAttack').addEventListener('click',()=>{ try{ const attack=game.resolveVehicleAttack({weaponIndex:Number($('vehicleWeaponSelect').value)}); renderVehicleAttack(attack); render(); }catch(e){const el=$('vehicleAttackResult');el.className='result fail';el.textContent=e.message;} });
$('applyVehicleDamage').addEventListener('click',()=>{ try{ const r=game.applyVehicleDamage({damage:Number($('vehicleIncomingDamage').value),breach:Number($('vehicleIncomingBreach').value)}); showFeedback('vehicleRepairFeedback',`Casco sofreu ${r.suffered}. Estado: ${r.status.label}.`); render(); }catch(e){showFeedback('vehicleRepairFeedback',e.message);} });
$('applyVehicleStrain').addEventListener('click',()=>{ try{ const r=game.applyVehicleStrain(Number($('vehicleIncomingStrain').value)); showFeedback('vehicleRepairFeedback',`+${r.suffered} de Tensão de sistemas. Estado: ${r.status.label}.`); render(); }catch(e){showFeedback('vehicleRepairFeedback',e.message);} });
$('vehicleDamageControl').addEventListener('click',()=>{ try{ const r=game.vehicleDamageControl(); showFeedback('vehicleRepairFeedback',r.check.success?`Controle de danos: ${r.systemStrainRecovered} Tensão e ${r.hullRecovered} Casco recuperados.`:`Falha no controle de danos. Dificuldade ${r.difficulty}.`); render(); }catch(e){showFeedback('vehicleRepairFeedback',e.message);} });
$('vehicleCritical').addEventListener('click',()=>{ try{ const r=game.applyActiveVehicleCritical(); showFeedback('vehicleRepairFeedback',r.hit.effect); render(); }catch(e){showFeedback('vehicleRepairFeedback',e.message);} });
$('vehicleHazardCheck').addEventListener('click',()=>{ try{ const r=game.vehiclePilotingHazardCheck(); showFeedback('vehicleRepairFeedback',`Pilotagem: ${r.check.success?'sucesso':'falha'} · dificuldade ${r.difficulty} com ${r.upgrades} melhoria(s).`); render(); }catch(e){showFeedback('vehicleRepairFeedback',e.message);} });
$('vehicleRestDay').addEventListener('click',()=>{ try{ const r=game.restVehicleFullDay(); showFeedback('vehicleRepairFeedback',`Dia de recuperação: ${r.recovered} de Tensão de sistemas removido.`); render(); }catch(e){showFeedback('vehicleRepairFeedback',e.message);} });
$('vehicleAstrogation').addEventListener('click',()=>{ try{ const r=game.plotHyperspaceCourse(); const tempo=r.timeMultiplier===0.5?' Cartógrafo galáctico: cálculo consome metade do tempo habitual.':''; showFeedback('vehicleTravelFeedback',`Astronavegação Fácil: ${r.check.success?'rota calculada':'cálculo falhou'}.${tempo}`); render(); }catch(e){showFeedback('vehicleTravelFeedback',e.message);} });

$('imageMapSearch').addEventListener('input',renderMapLibrary);
$('imageMapCatalog').addEventListener('click',e=>{ const b=e.target.closest('[data-map-asset]'); if(!b) return; mapLibraryUi.selectedAssetId=b.dataset.mapAsset; renderMapLibrary(); });
$('imageMapCache').addEventListener('click',cacheSelectedMap);
$('imageMapClearCache').addEventListener('click',clearSelectedMapCache);
$('imageMapRefresh').addEventListener('click',()=>{ mapLibraryUi.statuses.clear(); renderMapLibrary(); });
renderMapLibrary();

$('imageVisualSearch').addEventListener('input',renderVisualLibrary);
$('imageVisualKind').addEventListener('change',renderVisualLibrary);
$('imageVisualCatalog').addEventListener('click',e=>{ const b=e.target.closest('[data-visual-asset]'); if(!b) return; visualLibraryUi.selectedAssetId=b.dataset.visualAsset; renderVisualLibrary(); });
$('imageVisualCache').addEventListener('click',cacheSelectedVisualAsset);
$('imageVisualClearCache').addEventListener('click',clearSelectedVisualAssetCache);
$('imageVisualRefresh').addEventListener('click',()=>{ mapLibraryUi.statuses.clear(); renderVisualLibrary(); });
renderVisualLibrary();

$('mapZoomIn').addEventListener('click',()=>zoomGalaxyMap(.5));
$('mapZoomOut').addEventListener('click',()=>zoomGalaxyMap(-.5));
$('mapReset').addEventListener('click',resetGalaxyMap);
$('mapPlotCourse').addEventListener('click',()=>{
  try{
    const destination=$('mapDestination').value.trim(); if(!destination) throw new Error('Informe um destino antes de calcular a rota.');
    const result=game.plotHyperspaceCourse();
    showFeedback('mapTravelFeedback',`${result.check.success?'Rota calculada':'Cálculo da rota falhou'} para ${destination}. A viagem ainda precisa ser confirmada pelo Mestre.`);
    render();
  }catch(e){ showFeedback('mapTravelFeedback',e.message); }
});
$('mapProposeTravel').addEventListener('click',()=>{
  try{
    const destination=$('mapDestination').value.trim(); if(!destination) throw new Error('Informe um destino para propor a viagem.');
    const result=game.submitIntent(`Quero viajar para ${destination}. Avalie a rota, o tempo, os riscos e os requisitos antes de alterar nossa localização.`);
    showFeedback('mapTravelFeedback',result.master.playerMessage);
    render();
  }catch(e){ showFeedback('mapTravelFeedback',e.message); }
});
const mapViewport=$('galaxyMapViewport');
mapViewport.addEventListener('pointerdown',e=>{ galaxyMapUi.dragging=true; galaxyMapUi.pointerId=e.pointerId; galaxyMapUi.lastX=e.clientX; galaxyMapUi.lastY=e.clientY; mapViewport.classList.add('dragging'); mapViewport.setPointerCapture?.(e.pointerId); });
mapViewport.addEventListener('pointermove',e=>{ if(!galaxyMapUi.dragging||e.pointerId!==galaxyMapUi.pointerId||galaxyMapUi.scale<=1) return; galaxyMapUi.x+=e.clientX-galaxyMapUi.lastX; galaxyMapUi.y+=e.clientY-galaxyMapUi.lastY; galaxyMapUi.lastX=e.clientX; galaxyMapUi.lastY=e.clientY; applyGalaxyMapTransform(); });
const finishMapDrag=e=>{ if(e.pointerId!==galaxyMapUi.pointerId) return; galaxyMapUi.dragging=false; galaxyMapUi.pointerId=null; mapViewport.classList.remove('dragging'); };
mapViewport.addEventListener('pointerup',finishMapDrag); mapViewport.addEventListener('pointercancel',finishMapDrag);
mapViewport.addEventListener('wheel',e=>{ e.preventDefault(); zoomGalaxyMap(e.deltaY<0?.25:-.25); },{passive:false});


$('catalogWeapons').addEventListener('click',e=>{
  const button=e.target.closest('.propose-weapon'); if(!button) return;
  try{ const result=game.proposeWeaponAcquisition(button.dataset.itemId); showFeedback('equipmentCatalogFeedback',result.master.playerMessage); render(); }
  catch(err){ showFeedback('equipmentCatalogFeedback',err.message); }
});

$('talentOptions').addEventListener('click',e=>{
  const button=e.target.closest('.buy-talent'); if(!button) return;
  try{ const row=button.closest('.talent-row'); const characteristicKey=row?.querySelector('.talent-characteristic')?.value ?? null; const r=game.buyTalent(button.dataset.talentId,{characteristicKey}); showFeedback('talentFeedback',`${r.talent.name} adquirido por ${r.talent.cost} PE.`); render(); }
  catch(err){ showFeedback('talentFeedback',err.message); }
});
$('saveGroupBase').addEventListener('click',()=>{ try{ game.updateGroupBase({name:$('groupBaseName').value,location:$('groupBaseLocation').value,description:$('groupBaseDescription').value}); showFeedback('groupBaseFeedback','Refúgio / Base salvo.'); render(); }catch(e){showFeedback('groupBaseFeedback',e.message);} });
$('addGroupObligation').addEventListener('click',()=>{ try{ game.addGroupObligation({value:Number($('groupObligationValue').value),member:$('groupObligationMember').value,type:$('groupObligationType').value,description:$('groupObligationDescription').value}); showFeedback('groupObligationFeedback','Obligation adicionada ao grupo.'); render(); }catch(e){showFeedback('groupObligationFeedback',e.message);} });
$('groupObligationEntries').addEventListener('click',e=>{ const b=e.target.closest('.remove-group-obligation'); if(!b) return; try{game.removeGroupObligation(b.dataset.obligationId); showFeedback('groupObligationFeedback','Obligation removida.'); render();}catch(err){showFeedback('groupObligationFeedback',err.message);} });
$('saveGroupLists').addEventListener('click',()=>{ try{ game.setGroupList('resources',linesFromTextarea('groupResources')); game.setGroupList('belongings',linesFromTextarea('groupBelongings')); game.setGroupList('contacts',linesFromTextarea('groupContacts')); showFeedback('groupListsFeedback','Recursos, Pertences e Contatos salvos.'); render(); }catch(e){showFeedback('groupListsFeedback',e.message);} });
$('saveGroupDestiny').addEventListener('click',()=>{ try{ game.setGroupDestiny({light:Number($('groupDestinyLight').value),dark:Number($('groupDestinyDark').value)}); showFeedback('groupDestinyFeedback','Reserva de Destino salva.'); render(); }catch(e){showFeedback('groupDestinyFeedback',e.message);} });

$('newCampaign').addEventListener('click', () => {
  if (confirm('Criar nova campanha local? O estado atual será substituído.')) {
    const name = prompt('Nome da campanha:', 'Nova campanha') ?? 'Nova campanha';
    game.newCampaign(name); render();
  }
});

$('masterProviderMode').addEventListener('change',()=>{ $('masterEndpointWrap').hidden=$('masterProviderMode').value!=='http'; });
$('saveMasterSettings').addEventListener('click',()=>{
  try {
    const settings=game.configureMasterProvider({mode:$('masterProviderMode').value,endpoint:$('masterEndpoint').value});
    game.configureTts({enabled:$('ttsEnabled').checked,rate:Number($('ttsRate').value)});
    showFeedback('masterSettingsFeedback',settings.masterProvider==='http'?'Ajustes salvos; Mestre remoto configurado sem armazenar chave.':'Ajustes salvos; modo local ativo.');
    render();
  } catch(e) { showFeedback('masterSettingsFeedback',e.message); }
});
$('ttsPlayNarration').addEventListener('click',()=>{
  try {
    if(!game.state.settings?.ttsEnabled) throw new Error('Ative a voz da narração em Ajustes.');
    const result=speakNarration(game.state.scene?.narration,{rate:game.state.settings?.ttsRate ?? 1});
    showFeedback('ttsFeedback',`Lendo a narração em PT-BR · velocidade ${result.rate}.`);
  } catch(e){ showFeedback('ttsFeedback',e.message); }
});
$('ttsStopNarration').addEventListener('click',()=>{ stopNarration(); showFeedback('ttsFeedback','Voz interrompida.'); });

$('importLibraryPdf').addEventListener('click',()=>{
  try { requestPdfImport(); showFeedback('libraryImportFeedback','Selecione um PDF. Ele será copiado e indexado em segundo plano dentro do aplicativo.'); }
  catch(e) { showFeedback('libraryImportFeedback',e.message); }
});

$('searchLibrary').addEventListener('click',()=>{
  try {
    const query=$('libraryQuery').value.trim();
    if (!query) throw new Error('Digite o que deseja buscar nos PDFs.');
    const results=searchImportedLibrary(query,{limit:10});
    renderLibrarySearch(results);
    showFeedback('librarySearchFeedback',results.length?`${results.length} trecho(s) encontrado(s) no índice offline.`:'Nenhum trecho encontrado.');
  } catch(e) { showFeedback('librarySearchFeedback',e.message); }
});
$('libraryQuery').addEventListener('keydown',e=>{ if(e.key==='Enter'){ e.preventDefault(); $('searchLibrary').click(); } });
$('libraryDocuments').addEventListener('change',e=>{
  const row=e.target.closest('.library-row');
  if (!row || !e.target.matches('.library-authority,.library-enabled')) return;
  const authorityType=row.querySelector('.library-authority').value;
  const enabled=row.querySelector('.library-enabled').checked;
  try {
    updateLibraryDocumentPolicy(row.dataset.docId,{authorityType,enabled});
    showFeedback('libraryImportFeedback',`Fonte atualizada: ${libraryAuthorityTypeLabel(authorityType)} · ${enabled?'ativa no Mestre':'desativada no Mestre'}.`);
    renderLibrary();
  } catch(err) { showFeedback('libraryImportFeedback',err.message); renderLibrary(); }
});

$('libraryDocuments').addEventListener('click',e=>{
  const reindex=e.target.closest('.library-reindex');
  if (reindex) {
    try { reindexLibraryDocument(reindex.dataset.docId); showFeedback('libraryImportFeedback','Reindexação iniciada. O PDF permanece disponível durante o processo.'); renderLibrary(); }
    catch(err) { showFeedback('libraryImportFeedback',err.message); }
    return;
  }
  const button=e.target.closest('.library-remove'); if(!button) return;
  if (!confirm('Remover este PDF e seu índice offline da Biblioteca?')) return;
  try { removeLibraryDocument(button.dataset.docId); showFeedback('libraryImportFeedback','PDF removido da Biblioteca local.'); renderLibrary(); }
  catch(err) { showFeedback('libraryImportFeedback',err.message); }
});

window.braseiroLibraryNativeEvent = raw => {
  let event={}; try { event=JSON.parse(String(raw ?? '{}')); } catch {}
  const type=event.type; const doc=event.payload?.document;
  const messages={
    cancelado:'Importação cancelada.',
    indexando:doc?`Anexando e indexando ${doc.fileName ?? 'PDF'}...`:'Indexação iniciada.',
    concluido:doc?.status==='INDEXADO'?`${doc.fileName} pronto para uso offline.`:`${doc?.fileName ?? 'PDF'} foi anexado, mas não contém texto extraível.`,
    duplicado:'Este PDF já existe na Biblioteca. O arquivo duplicado não foi criado.',
    removido:'PDF removido da Biblioteca.',
    politica:doc?`Autoridade de ${doc.fileName ?? 'PDF'} atualizada.`:'Autoridade da fonte atualizada.',
    erro:event.payload?.message || 'Falha ao importar o PDF.'
  };
  showFeedback('libraryImportFeedback',messages[type] ?? 'Biblioteca atualizada.');
  renderLibrary();
};

for (const tab of document.querySelectorAll('.tab')) {
  tab.addEventListener('click',()=>{
    document.querySelectorAll('.tab').forEach(t=>t.classList.toggle('active',t===tab));
    document.querySelectorAll('.view').forEach(v=>v.classList.toggle('active',v.id===tab.dataset.view));
  });
}

initCharacterCreator();
initVehicleProfiles();
render();
const initialView=location.hash.replace(/^#/,'');
if(initialView){
  const tab=[...document.querySelectorAll('.tab')].find(t=>t.dataset.view===initialView);
  if(tab){ document.querySelectorAll('.tab').forEach(t=>t.classList.toggle('active',t===tab)); document.querySelectorAll('.view').forEach(v=>v.classList.toggle('active',v.id===initialView)); }
}
