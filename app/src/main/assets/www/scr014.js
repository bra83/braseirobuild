import { NativePersistencePort, MemoryPersistenceAdapter } from './core/persistence-port.js';
import { projectPartyForPlayer, createPartyState } from './core/party-state.js';
import { PartyService, partyGroupsAreColocated } from './core/party-service.js';
import { resolveActiveCampaignId } from './core/runtime-campaign.js';
import { canonicalJson } from './core/canonical-json.js';
import { makeStarterCampaignState } from './core/starter-bootstrap.js';
import { TERRAIN_MPH } from './core/wwn-travel-core.js';
import { WorldActionService } from './core/world-service.js';
import { createRuntimeNarrativeProvider } from './core/runtime-narrative-provider.js';
import { createNativeTtsProvider } from './core/native-tts-provider.js';
import { TtsPresentationService } from './core/tts-presentation.js';

const HEX_W=92,HEX_H=106,STEP_X=92,STEP_Y=79.5;
const ORIGIN={q:17,r:22};
const canvas=document.getElementById('mapCanvas');const map=document.getElementById('map');const fogRevealMask=document.getElementById('fogRevealMask');
let zoom=1,pan={x:-225,y:-75},dragging=false,moved=false,last={x:0,y:0};
let state=null,partyProjection=null,selectedGroupId=null,persistence=null,campaignId=null,partyService=null,worldService=null,selectedHexKey='0,0';
const barbara=createRuntimeNarrativeProvider({native:globalThis.BraseiroNative});
const tts=new TtsPresentationService({provider:createNativeTtsProvider({native:globalThis.BraseiroNative})});
const RUGGED_HILLS_BASE_MPH=TERRAIN_MPH.DENSE_FOREST_OR_RUGGED_HILLS;
const WWN_TERRAIN_ROOT='assets/wwn_asset_library_v1/hex/terrain_base_cropped';
const WWN_TERRAIN_ASSETS=Object.freeze({
  LUSH_GRASSLAND:`${WWN_TERRAIN_ROOT}/terrain_base_01_lush_grassland.png`,
  DRY_GRASSLAND:`${WWN_TERRAIN_ROOT}/terrain_base_02_dry_grassland.png`,
  CULTIVATED_FARMLAND:`${WWN_TERRAIN_ROOT}/terrain_base_03_cultivated_farmland.png`,
  WILD_MEADOW:`${WWN_TERRAIN_ROOT}/terrain_base_04_wild_meadow.png`,
  SCRUBLAND:`${WWN_TERRAIN_ROOT}/terrain_base_05_scrubland.png`,
  DECIDUOUS_FOREST:`${WWN_TERRAIN_ROOT}/terrain_base_06_deciduous_forest.png`,
  DENSE_PINE_FOREST:`${WWN_TERRAIN_ROOT}/terrain_base_07_dense_pine_forest.png`,
  HUMID_JUNGLE:`${WWN_TERRAIN_ROOT}/terrain_base_08_humid_jungle.png`,
  REED_MARSH:`${WWN_TERRAIN_ROOT}/terrain_base_09_reed_marsh.png`,
  DARK_SWAMP:`${WWN_TERRAIN_ROOT}/terrain_base_10_dark_swamp.png`,
  SANDY_DESERT:`${WWN_TERRAIN_ROOT}/terrain_base_11_sandy_desert.png`,
  RED_BADLANDS:`${WWN_TERRAIN_ROOT}/terrain_base_12_red_badlands.png`,
  COLD_TUNDRA:`${WWN_TERRAIN_ROOT}/terrain_base_13_cold_tundra.png`,
  SNOWFIELD:`${WWN_TERRAIN_ROOT}/terrain_base_14_snowfield.png`,
  BLACK_VOLCANIC_WASTE:`${WWN_TERRAIN_ROOT}/terrain_base_15_black_volcanic_waste.png`,
  ROCKY_HIGHLAND:`${WWN_TERRAIN_ROOT}/terrain_base_16_rocky_highland.png`,
});

const QA_FOG_MODE=globalThis.__WWN_QA_FOG__===true||new URLSearchParams(location.search).get('qaFog')==='1';
function qaAtlasHexes(){
  if(!QA_FOG_MODE)return null;
  const rec=(q,r,terrain,ruleTerrain,tile,title,label,explored=false)=>({q,r,terrain,ruleTerrain,tile,terrainTitle:title,terrainLabel:label,terrainDescription:'Terreno conhecido neste hex de teste.',discovered:true,explored,visited:explored});
  return {
    '17,22':{q:17,r:22,terrain:'hills',ruleTerrain:'DENSE_FOREST_OR_RUGGED_HILLS',tile:WWN_TERRAIN_ASSETS.ROCKY_HIGHLAND,detailImage:'assets/wwn_asset_library_v1/scenes/scene_wilderness_01_grassland_road_ruins.png',terrainTitle:'RUÍNAS ROCHOSAS',terrainLabel:'COLINAS / RUÍNAS',terrainDescription:'Colinas acidentadas e ruínas conhecidas neste hex.',discovered:true,explored:false,visited:true,poi:'assets/wwn_asset_library_v1/poi/poi_overgrown_ruins_marker.png'},
    '16,22':rec(16,22,'forest','LIGHT_FOREST_OR_DESERT',WWN_TERRAIN_ASSETS.DENSE_PINE_FOREST,'FLORESTA DENSA','FLORESTA',false),
    '15,22':rec(15,22,'marsh','SWAMP_OR_MARSH',WWN_TERRAIN_ASSETS.REED_MARSH,'PÂNTANO DE JUNCOS','PÂNTANO',false),
    '18,22':rec(18,22,'highland','DENSE_FOREST_OR_RUGGED_HILLS',WWN_TERRAIN_ASSETS.ROCKY_HIGHLAND,'TERRAS ALTAS ROCHOSAS','COLINAS',true),
    '19,22':rec(19,22,'highland','MOUNTAINS_OR_DIRE_WASTELANDS',WWN_TERRAIN_ASSETS.ROCKY_HIGHLAND,'ESCARPAS ROCHOSAS','MONTANHAS',false),
    '17,21':rec(17,21,'grassland','PLAINS',WWN_TERRAIN_ASSETS.LUSH_GRASSLAND,'CAMPO VERDEJANTE','PLANÍCIE',true),
    '18,21':rec(18,21,'scrubland','DENSE_FOREST_OR_RUGGED_HILLS',WWN_TERRAIN_ASSETS.SCRUBLAND,'MATAGAL SECO','COLINAS / MATAGAL',false),
    '16,23':rec(16,23,'meadow','PLAINS',WWN_TERRAIN_ASSETS.WILD_MEADOW,'PRADO SELVAGEM','PLANÍCIE',true),
    '17,23':rec(17,23,'farmland','PLAINS',WWN_TERRAIN_ASSETS.CULTIVATED_FARMLAND,'CAMPOS CULTIVADOS','CAMPOS',false),
  };
}

async function browserStarterState(){
  const [pack,character]=await Promise.all([
    fetch('data/wwn.starter-region.v1.json').then(r=>r.json()),
    fetch('data/wwn.starter-character.v1.json').then(r=>r.json()),
  ]);
  let state=makeStarterCampaignState({character,pack});
  if(QA_FOG_MODE)state.worldState.atlas={...(state.worldState.atlas??{}),hexes:qaAtlasHexes()??{}};
  if(new URLSearchParams(location.search).get('qaSplit')==='1'){
    const tor={characterId:'tor',name:'TOR',classId:'WARRIOR',level:1,attributes:{Strength:12,Dexterity:10,Constitution:13,Intelligence:8,Wisdom:9,Charisma:10},hp:{current:7,max:7},systemStrain:{current:3,max:13},skills:{Stab:1}};
    state.charactersState={...(state.charactersState??{}),tor};
    state.partyState={schemaVersion:1,activeGroupId:'g1',groups:{g1:{groupId:'g1',label:'IRIA',memberIds:['iria'],position:{q:17,r:22,name:'RUÍNAS DA VIGÍLIA'},encounterRiskUnit:true},g2:{groupId:'g2',label:'TOR',memberIds:['tor'],position:{q:18,r:22,name:'HEX ADJACENTE'},encounterRiskUnit:true}}};
  }
  return state;
}
async function buildBrowserRuntime(){
  const persistence=new MemoryPersistenceAdapter();const state=await browserStarterState();
  persistence.createCampaign({campaignId:state.campaignId,displayName:state.displayName,stateJson:canonicalJson(state),masterSeed:state.rngState?.masterSeed??'braseiro-wwn-browser',rngAlgorithm:state.rngState?.algorithm??'xoshiro128ss-v1',initialSnapshotId:`snapshot:${state.campaignId}:r0`});
  return {persistence,campaignId:state.campaignId,state};
}
async function loadRuntime(){
  if(globalThis.BraseiroNative){try{const p=new NativePersistencePort();const id=resolveActiveCampaignId(p);if(id){const loaded=p.loadCampaign(id);if(loaded.status==='OK')return {persistence:p,campaignId:id,state:JSON.parse(loaded.stateJson)};}}catch{}}
  return buildBrowserRuntime();
}
function reloadState(){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')throw new Error(`campaign reload failed: ${loaded.status}`);state=JSON.parse(loaded.stateJson);partyProjection=projectPartyForPlayer(state);if(!partyProjection.groups?.[selectedGroupId])selectedGroupId=partyProjection.activeGroupId??Object.keys(partyProjection.groups??{})[0]??null;}

function pos(q,r){return{x:STEP_X*(q+r/2)+360,y:STEP_Y*r+300}}
const HEX_POLYGON=[[46,0],[92,26.5],[92,79.5],[46,106],[0,79.5],[0,26.5]];
function worldHexPoints(q,r,expand=1){const p=pos(q,r),cx=46,cy=53;return HEX_POLYGON.map(([x,y])=>`${(p.x+cx+(x-cx)*expand).toFixed(1)},${(p.y+cy+(y-cy)*expand).toFixed(1)}`).join(' ')}
let applyFrame=0;
function applyNow(){canvas.style.transform=`translate(${pan.x}px,${pan.y}px) scale(${zoom})`;if(fogRevealMask)fogRevealMask.setAttribute('transform',`translate(${pan.x} ${pan.y}) scale(${zoom})`)}
function apply(){applyNow()}
function scheduleApply(){if(applyFrame)return;applyFrame=requestAnimationFrame(()=>{applyFrame=0;applyNow()})}
function relPos(){const p=state?.worldState?.position??{};return{q:Number.isInteger(p.q)?p.q-ORIGIN.q:0,r:Number.isInteger(p.r)?p.r-ORIGIN.r:0}}
function groupRelPosition(group){const gp=group?.position??state?.worldState?.position??{};return{q:Number.isInteger(gp.q)?gp.q-ORIGIN.q:0,r:Number.isInteger(gp.r)?gp.r-ORIGIN.r:0}}
function hexKey(q,r){return `${q},${r}`}
function absoluteHexKey(q,r){return `${ORIGIN.q+q},${ORIGIN.r+r}`}
function persistedAtlasHex(q,r){return state?.worldState?.atlas?.hexes?.[absoluteHexKey(q,r)]??state?.atlasState?.hexes?.[absoluteHexKey(q,r)]??null}
function discoveryState(rec,current=false){if(current)return'CURRENT';if(rec?.explored===true)return'EXPLORED';if(rec?.discovered===true||rec?.known===true||rec?.seen===true||rec?.entered===true||rec?.visited===true)return'DISCOVERED';return'UNKNOWN'}
function poiHasBeenRevealed(raw,current=false){
  // A POI is not revealed merely because the terrain is discovered. It becomes
  // player-visible only after a party/hero has actually entered/visited the hex.
  return Boolean(current||raw?.visited===true||raw?.entered===true||raw?.poiRevealed===true||raw?.explored===true);
}
function currentHexRecord(q,r,current){
  const raw=persistedAtlasHex(q,r),status=discoveryState(raw,current);
  if(status==='UNKNOWN')return{known:false,status};
  if(raw){const poiRevealed=poiHasBeenRevealed(raw,current);return{known:true,status,terrain:raw.terrainLabel??String(raw.terrain??'TERRENO').toUpperCase(),terrainTitle:raw.terrainTitle??raw.title??String(raw.terrain??'TERRENO').toUpperCase(),terrainDescription:raw.terrainDescription??raw.description??'Terreno conhecido neste hex.',tile:raw.tile??null,detailImage:raw.detailImage??raw.sceneImage??null,poi:poiRevealed?(raw.poi??null):null,poiName:poiRevealed?(raw.poiName??null):null,poiType:poiRevealed?(raw.poiType??null):null,poiRevealed,ruleTerrain:raw.ruleTerrain??null,state:status==='EXPLORED'?'EXPLORADO':'DESCOBERTO',placeName:raw.placeName??null}}
  return{known:true,status:'CURRENT',terrain:'COLINAS / RUÍNAS',terrainTitle:'RUÍNAS ROCHOSAS',terrainDescription:'Colinas acidentadas e ruínas conhecidas neste hex.',tile:WWN_TERRAIN_ASSETS.ROCKY_HIGHLAND,detailImage:'assets/wwn_asset_library_v1/scenes/scene_wilderness_01_grassland_road_ruins.png',poi:'assets/wwn_asset_library_v1/poi/poi_overgrown_ruins_marker.png',poiRevealed:true,ruleTerrain:'DENSE_FOREST_OR_RUGGED_HILLS',state:'DESCOBERTO',placeName:state?.worldState?.position?.name??'RUÍNAS DA VIGÍLIA'};
}
function movementForRecord(rec){if(!rec?.known||!rec.ruleTerrain)return'—';const mph=TERRAIN_MPH[rec.ruleTerrain];return Number.isFinite(mph)?`${String(mph).replace('.',',')} mi/h`:'—'}
function refreshFogMask(){
  if(!fogRevealMask||!state)return;
  const targets=new Map();const current=relPos();
  for(let q=-4;q<=4;q++)for(let r=-4;r<=4;r++)if(Math.abs(q+r)<=5){const rec=currentHexRecord(q,r,q===current.q&&r===current.r);if(!rec.known)continue;const strength=rec.status==='CURRENT'?1:rec.status==='EXPLORED'?.9:.62;targets.set(hexKey(q,r),{q,r,strength,status:rec.status});}
  const existing=new Map([...fogRevealMask.querySelectorAll('g[data-fog-key]')].map(g=>[g.dataset.fogKey,g]));
  for(const [key,t] of targets){let g=existing.get(key);if(!g){g=document.createElementNS('http://www.w3.org/2000/svg','g');g.dataset.fogKey=key;const soft=document.createElementNS('http://www.w3.org/2000/svg','polygon');soft.setAttribute('class','fog-hole-soft');soft.setAttribute('fill','black');soft.setAttribute('filter','url(#fogFeather)');soft.style.opacity='0';const core=document.createElementNS('http://www.w3.org/2000/svg','polygon');core.setAttribute('class','fog-hole-core');core.setAttribute('fill','black');core.style.opacity='0';g.append(soft,core);fogRevealMask.appendChild(g);requestAnimationFrame(()=>{soft.style.opacity=String(Math.min(1,t.strength+.1));core.style.opacity=String(t.strength)});}else existing.delete(key);const [soft,core]=g.children;soft.setAttribute('points',worldHexPoints(t.q,t.r,1.18));core.setAttribute('points',worldHexPoints(t.q,t.r,1.02));soft.style.opacity=String(Math.min(1,t.strength+.1));core.style.opacity=String(t.strength);}
  for(const g of existing.values()){for(const el of g.children)el.style.opacity='0';setTimeout(()=>g.remove(),760)}
}
function updateTerrainInfo(q,r,current){
  const rec=currentHexRecord(q,r,current);const qAbs=ORIGIN.q+q,rAbs=ORIGIN.r+r;
  const thumb=document.getElementById('terrainThumb'),partyButton=document.getElementById('openParty');
  document.getElementById('terrainKicker').textContent=current?'TERRENO ATUAL':rec.known?'TERRENO CONHECIDO':'HEX SELECIONADO';
  document.getElementById('terrainTitle').textContent=rec.known?rec.terrainTitle:'DESCONHECIDO';
  document.getElementById('terrainDescription').textContent=rec.known?rec.terrainDescription:'Este hex ainda não foi descoberto. Selecioná-lo não revela terreno nem conteúdo oculto.';
  document.getElementById('terrainType').textContent=rec.known?rec.terrain:'—';
  document.getElementById('terrainMove').textContent=movementForRecord(rec);
  document.getElementById('terrainState').textContent=rec.known?rec.state:'NÃO EXPLORADO';
  document.getElementById('placeName').textContent=rec.placeName??(current?(state?.worldState?.position?.name??'RUÍNAS DA VIGÍLIA'):`Q${qAbs} • R${rAbs}`);
  if(rec.known&&(rec.detailImage||rec.tile)){thumb.src=rec.detailImage??rec.tile;}else{thumb.src='data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs=';}thumb.classList.toggle('unknown',!rec.known);thumb.alt=rec.known?(rec.detailImage?'Paisagem do terreno selecionado':'Terreno conhecido do hex'):'Terreno oculto';
  partyButton.hidden=!current;
  const travel=document.getElementById('travelHex'),explore=document.getElementById('exploreHex');
  const cur=relPos();const distance=(Math.abs(q-cur.q)+Math.abs(r-cur.r)+Math.abs((q-cur.q)+(r-cur.r)))/2;
  travel.hidden=current;travel.disabled=!(!current&&distance===1);
  explore.hidden=!current;explore.disabled=!(current&&rec.known&&rec.status!=='EXPLORED');
  if(!current&&distance!==1)document.getElementById('worldActionStatus').textContent='Selecione um hex adjacente para viajar.';
  else if(current&&rec.status==='EXPLORED')document.getElementById('worldActionStatus').textContent='Este hex já foi explorado.';
  else document.getElementById('worldActionStatus').textContent='';
}
function renderMap(){
  canvas.innerHTML='';const current=relPos();
  for(let q=-4;q<=4;q++)for(let r=-4;r<=4;r++)if(Math.abs(q+r)<=5){
    const p=pos(q,r),isCurrent=q===current.q&&r===current.r,k=hexKey(q,r),rec=currentHexRecord(q,r,isCurrent);const h=document.createElement('button');
    h.className='hex '+(rec.known?`seen ${rec.status.toLowerCase()}`:'unknown')+(isCurrent?' current':'')+(k===selectedHexKey?' selected':'');h.style.left=p.x+'px';h.style.top=p.y+'px';h.dataset.hexKey=k;h.dataset.discovery=rec.status;h.ariaLabel=rec.known?`Hex conhecido: ${rec.terrainTitle}`:'Hex desconhecido';
    if(rec.known&&rec.tile)h.style.setProperty('--terrain-image',`url("${rec.tile}")`);if(rec.known&&(rec.poi||rec.poiName)){const poiWrap=document.createElement('span');poiWrap.className='poi-structure';poiWrap.setAttribute('aria-hidden','true');if(rec.poi){const poi=document.createElement('img');poi.className='poi-structure-art';poi.src=rec.poi;poi.alt='';poiWrap.appendChild(poi)}else{poiWrap.classList.add('poi-symbol');const label=document.createElement('i');label.textContent=String(rec.poiName??'POI').slice(0,18);poiWrap.appendChild(label)}h.appendChild(poiWrap)}
    h.addEventListener('click',()=>{if(moved)return;canvas.querySelector('.hex.selected')?.classList.remove('selected');selectedHexKey=k;h.classList.add('selected');updateTerrainInfo(q,r,isCurrent)});canvas.appendChild(h)
  }
  const groups=Object.values(partyProjection?.groups??{});groups.forEach((group,index)=>{const gp=groupRelPosition(group),cp=pos(gp.q,gp.r);const token=document.createElement('button');token.className='group-token';token.dataset.groupId=group.groupId;token.style.left=(cp.x+48+(index%2)*12)+'px';token.style.top=(cp.y+58+Math.floor(index/2)*12)+'px';token.innerHTML=`<img src="assets/atlas_repo/group_token_adventurers.png" alt=""><span class="group-index">${groups.length>1?index+1:''}</span>`;token.setAttribute('aria-label',`Abrir grupo ${group.label??''}`.trim());token.addEventListener('pointerdown',e=>e.stopPropagation());token.addEventListener('click',e=>{e.stopPropagation();if(!moved)openParty(group.groupId)});canvas.appendChild(token)});refreshFogMask();apply();
}
function activeGroup(){return partyProjection?.groups?.[selectedGroupId??partyProjection.activeGroupId]??Object.values(partyProjection?.groups??{})[0]??null}
function masterVoiceEnabled(){try{const raw=globalThis.BraseiroNative?.audioStatus?.();const s=typeof raw==='string'?JSON.parse(raw):raw;return s?.masterVoiceEnabled!==false}catch{return true}}
function setWorldNarrative(text){const card=document.getElementById('worldNarrativeCard'),box=document.getElementById('worldNarrativeText');const clean=String(text??'').trim();if(!clean){card.hidden=true;box.textContent='';return;}box.textContent=clean;card.hidden=false;}
async function narrateWorldResult(result){
  if(!result?.receipt?.eventId)return null;const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return null;const st=JSON.parse(loaded.stateJson);const record=typeof persistence.getEvent==='function'?persistence.getEvent(result.receipt.eventId)?.event:null;let mechanicalEvent=null;try{mechanicalEvent=record?.payloadJson?JSON.parse(record.payloadJson):null}catch{}
  mechanicalEvent=mechanicalEvent??{eventId:result.receipt.eventId,revision:result.receipt.newRevision,kind:'WORLD_ACTION',mechanicalMutation:true};const direct=st.encounterState?.currentScene?.publicScene;if(direct&&!mechanicalEvent.scene)mechanicalEvent={...mechanicalEvent,scene:direct};
  const narrative=barbara.generateAsync?await barbara.generateAsync({mechanicalEvent,state:st}):await Promise.resolve(barbara.generate({mechanicalEvent,state:st}));if(!String(narrative?.text??'').trim())return narrative;
  persistence.appendNarrative({campaignId,eventId:`narrative:${result.receipt.eventId}`,mechanicalEventId:result.receipt.eventId,mechanicalRevision:result.receipt.newRevision,eventPayloadJson:canonicalJson(narrative)});setWorldNarrative(narrative.text);if(masterVoiceEnabled())void tts.speakVisible({text:narrative.text,visibility:'PLAYER_VISIBLE',rate:.96,pitch:.96,role:'NARRADOR'});return narrative;
}
function rawActiveGroup(){return state?.partyState?.groups?.[selectedGroupId??state.partyState?.activeGroupId]??null}
function setPartyStatus(text,error=false){const el=document.getElementById('partyStatus');el.textContent=text??'';el.classList.toggle('error',Boolean(error))}
function groupsAtSamePosition(groupId){const party=state?.partyState;if(!party?.groups?.[groupId])return[];return Object.keys(party.groups).filter(id=>id===groupId||partyGroupsAreColocated(party,[groupId,id]));}
function updatePartyActions(){const g=activeGroup(),raw=rawActiveGroup();const split=document.getElementById('splitParty'),merge=document.getElementById('mergeParty');split.hidden=!(g?.members?.length>1);const colocated=raw?groupsAtSamePosition(raw.groupId):[];merge.hidden=colocated.length<2;merge.dataset.groupIds=colocated.join(',');}
function closeSplitPanel(){const panel=document.getElementById('splitPanel');panel.setAttribute('aria-hidden','true');document.getElementById('splitMembers').innerHTML='';setPartyStatus('')}
function openSplitPanel(){const g=activeGroup();if(!g||g.members.length<2)return;const box=document.getElementById('splitMembers');box.innerHTML='';g.members.forEach((m,index)=>{const label=document.createElement('label');label.className='split-choice';const input=document.createElement('input');input.type='checkbox';input.value=m.characterId;input.checked=index===g.members.length-1;const span=document.createElement('span');span.textContent=m.name;label.append(input,span);box.appendChild(label)});document.getElementById('splitPanel').setAttribute('aria-hidden','false');setPartyStatus('Os dois subgrupos permanecem neste hex até uma ação de viagem válida.')}
function deterministicSplitIds(sourceId,revision){const safe=String(sourceId).replace(/[^a-z0-9_-]/gi,'-');return [`${safe}-a-r${revision+1}`,`${safe}-b-r${revision+1}`]}
function refreshPartyUi({keepPopup=true}={}){reloadState();renderMap();if(keepPopup){renderParty();updatePartyActions()}}
function renderParty(){const box=document.getElementById('partyMembers');box.innerHTML='';const g=activeGroup();document.getElementById('partyTitle').textContent=g?.label??'GRUPO';for(const m of g?.members??[]){const card=document.createElement('article');card.className='member';const bars=m.statusBars;const hp=bars?.hp,ss=bars?.systemStrain;const initials=(m.name??'?').slice(0,2).toUpperCase();card.innerHTML=`<div class="avatar">${initials}</div><div><div class="member-head"><b>${m.name}</b><span>${m.classId??''} ${m.level?`• N${m.level}`:''}</span></div>${hp?`<div class="barline"><label>PV</label><div class="track"><div class="fill hp" style="width:${Math.round(hp.fill*100)}%"></div></div><div class="value">${hp.current}/${hp.max}</div></div>`:''}${ss?`<div class="barline"><label>STRAIN</label><div class="track"><div class="fill strain" style="width:${Math.round(ss.fill*100)}%"></div></div><div class="value">${ss.current}/${ss.max}</div></div>`:''}</div>`;box.appendChild(card)}updatePartyActions()}
function openParty(groupId=null){selectedGroupId=groupId??partyProjection?.activeGroupId??null;renderParty();document.getElementById('shade').style.display='block';const p=document.getElementById('partyPopup');p.style.display='block';p.ariaHidden='false'}
function closeParty(){closeSplitPanel();document.getElementById('shade').style.display='none';const p=document.getElementById('partyPopup');p.style.display='none';p.ariaHidden='true'}
document.getElementById('openParty').onclick=()=>openParty();document.getElementById('closeParty').onclick=closeParty;document.getElementById('shade').onclick=closeParty;
document.getElementById('splitParty').onclick=openSplitPanel;document.getElementById('cancelSplit').onclick=closeSplitPanel;
document.getElementById('confirmSplit').onclick=()=>{const raw=rawActiveGroup(),g=activeGroup();if(!raw||!g)return;const selected=[...document.querySelectorAll('#splitMembers input:checked')].map(x=>x.value);if(selected.length<1||selected.length>=raw.memberIds.length){setPartyStatus('Escolha pelo menos um herói, mas não todos.',true);return;}const selectedSet=new Set(selected),remaining=raw.memberIds.filter(id=>!selectedSet.has(id));const [a,b]=deterministicSplitIds(raw.groupId,state.revision);const result=partyService.split({campaignId,sourceGroupId:raw.groupId,newGroups:[{groupId:a,label:remaining.length===1?(g.members.find(m=>m.characterId===remaining[0])?.name??'GRUPO A'):'GRUPO A',memberIds:remaining,position:raw.position},{groupId:b,label:selected.length===1?(g.members.find(m=>m.characterId===selected[0])?.name??'GRUPO B'):'GRUPO B',memberIds:selected,position:raw.position}]});if(result.status!=='OK'){setPartyStatus(result.status,true);return;}selectedGroupId=a;closeSplitPanel();refreshPartyUi();setPartyStatus('Grupo separado. Ambos permanecem no mesmo hex; agora são unidades de risco independentes.')};
document.getElementById('mergeParty').onclick=()=>{const ids=(document.getElementById('mergeParty').dataset.groupIds??'').split(',').filter(Boolean);if(ids.length<2){setPartyStatus('Não há outro subgrupo neste hex.',true);return;}const target=`party-merge-r${state.revision+1}`;const result=partyService.mergeColocated({campaignId,groupIds:ids,targetGroupId:target,label:'GRUPO'});if(result.status!=='OK'){setPartyStatus(result.status,true);return;}selectedGroupId=target;refreshPartyUi();setPartyStatus('Subgrupos reunidos no mesmo hex.')};
document.getElementById('travelHex').onclick=async()=>{
  const button=document.getElementById('travelHex'),[q,r]=selectedHexKey.split(',').map(Number),target={q:ORIGIN.q+q,r:ORIGIN.r+r};const status=document.getElementById('worldActionStatus');
  status.textContent='Viajando…';button.disabled=true;setWorldNarrative('');
  try{const result=worldService.travelToAdjacentHex({campaignId,...target});if(result.status!=='OK'){status.textContent=result.status==='TRAVEL_INTERRUPTED_DESTINATION_LOCKED_NO_MUTATION'?`Há uma viagem interrompida para Q${result.pendingTravel?.q} • R${result.pendingTravel?.r}. Resolva/retome esse trajeto antes de escolher outro destino.`:`Viagem não concluída: ${result.status}`;return;}reloadState();const rp=relPos();selectedHexKey=hexKey(rp.q,rp.r);renderMap();updateTerrainInfo(rp.q,rp.r,true);status.textContent=result.encounterOccurred?'Encontro no trajeto • Barbara está narrando…':'Destino alcançado • Barbara está narrando…';document.getElementById('coords').textContent=`Q${state.worldState?.position?.q??target.q} • R${state.worldState?.position?.r??target.r}`;const narrative=await narrateWorldResult(result);status.textContent=result.encounterOccurred?(result.interrupted?`ENCONTRO INTERROMPEU A VIAGEM • ${Number(result.remainingHours??0).toFixed(1)} h restantes • cena pronta na Jornada.`:'ENCONTRO NO TRAJETO • cena pronta na Jornada.'):(narrative?.text?'Destino alcançado • cena narrada.':'Destino alcançado.');}catch(error){status.textContent=`Falha na viagem: ${error?.message??error}`;}finally{button.disabled=false;}
};
document.getElementById('exploreHex').onclick=async()=>{
  const p=state?.worldState?.position??{},button=document.getElementById('exploreHex'),status=document.getElementById('worldActionStatus');if(!Number.isInteger(p.q)||!Number.isInteger(p.r))return;status.textContent='Explorando o hex…';button.disabled=true;setWorldNarrative('');
  try{const raw=state?.worldState?.atlas?.hexes?.[`${p.q},${p.r}`]??{};const multiplier=[1,2,3].includes(raw.explorationDays)?raw.explorationDays:1;const result=worldService.exploreHex({campaignId,q:p.q,r:p.r,concealmentMultiplier:multiplier});if(!['OK','ALREADY_EXPLORED_NO_MUTATION'].includes(result.status)){status.textContent=`Exploração não concluída: ${result.status}`;return;}reloadState();renderMap();const rp=relPos();updateTerrainInfo(rp.q,rp.r,true);if(result.status==='OK'){status.textContent=result.discovery?.poiName?`Descoberto: ${result.discovery.poiName} • Barbara está narrando…`:`Hex explorado • ${result.plan.days} dia(s) • Barbara está narrando…`;await narrateWorldResult(result);reloadState();renderMap();updateTerrainInfo(rp.q,rp.r,true);}else status.textContent='Este hex já estava explorado.';}catch(error){status.textContent=`Falha na exploração: ${error?.message??error}`;}finally{button.disabled=false;}
};
document.getElementById('openWorldScene').onclick=()=>{location.href='jornada.html'};
document.getElementById('zoomIn').onclick=()=>{zoom=Math.min(1.35,zoom+.12);apply()};document.getElementById('zoomOut').onclick=()=>{zoom=Math.max(.72,zoom-.12);apply()};document.getElementById('recenter').onclick=()=>{zoom=1;pan={x:-225,y:-75};apply()};
map.addEventListener('pointerdown',e=>{if(e.target.closest('.tools,.group-token'))return;dragging=true;moved=false;last={x:e.clientX,y:e.clientY};map.setPointerCapture(e.pointerId)});map.addEventListener('pointermove',e=>{if(!dragging)return;const dx=e.clientX-last.x,dy=e.clientY-last.y;if(Math.abs(dx)+Math.abs(dy)>2)moved=true;pan.x+=dx;pan.y+=dy;last={x:e.clientX,y:e.clientY};scheduleApply()});map.addEventListener('pointerup',()=>{dragging=false;setTimeout(()=>moved=false,0)});
const runtime=await loadRuntime();persistence=runtime.persistence;campaignId=runtime.campaignId;state=runtime.state;partyService=new PartyService({persistence});worldService=new WorldActionService({persistence});partyProjection=projectPartyForPlayer(state);selectedGroupId=partyProjection.activeGroupId??Object.keys(partyProjection.groups??{})[0]??null;const position=state.worldState?.position??{};document.getElementById('campaignName').textContent=state.displayName??state.contentState?.public?.campaign??'SOMBRAS DE GELO E FERRO';document.getElementById('placeName').textContent=position.name??'POSIÇÃO ATUAL';document.getElementById('coords').textContent=`Q${position.q??17} • R${position.r??22}`;renderMap();updateTerrainInfo(0,0,true);
function qaRevealHex(q,r,{terrain='highland',ruleTerrain='DENSE_FOREST_OR_RUGGED_HILLS',tile=WWN_TERRAIN_ASSETS.ROCKY_HIGHLAND,explored=false}={}){if(!QA_FOG_MODE)return{status:'QA_DISABLED'};state.worldState.atlas=state.worldState.atlas??{hexes:{}};state.worldState.atlas.hexes[absoluteHexKey(q,r)]={q:ORIGIN.q+q,r:ORIGIN.r+r,terrain,ruleTerrain,tile,terrainTitle:String(terrain).toUpperCase(),terrainLabel:String(terrain).toUpperCase(),terrainDescription:'Terreno conhecido neste hex de teste.',discovered:true,explored,visited:explored};renderMap();return{status:'OK',key:absoluteHexKey(q,r)}}
function refreshFromPersistence(){reloadState();renderMap();const [q,r]=selectedHexKey.split(',').map(Number);const current=relPos();updateTerrainInfo(q,r,q===current.q&&r===current.r);return{status:'OK'}}
window.__SCR014_RUNTIME__={state:()=>structuredClone(state),party:()=>structuredClone(partyProjection),openParty,split:({sourceGroupId,newGroups})=>partyService.split({campaignId,sourceGroupId,newGroups}),mergeColocated:({groupIds,targetGroupId,label})=>partyService.mergeColocated({campaignId,groupIds,targetGroupId,label}),refreshFromPersistence,qaRevealHex};
