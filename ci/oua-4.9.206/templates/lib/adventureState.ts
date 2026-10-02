import type { CampaignState } from './db';
import { loadMapDiscovery, saveMapDiscovery } from './map';
import { normalizeMasterState } from './master/state';
import { ADVENTURE_MODULES } from './adventureModules';
import { campaignStartLabel } from './adventureStartResolver';
import type { ResolvedCampaignStart } from './adventureTypes';

export async function applyResolvedCampaignStart(campaign:CampaignState,resolved:ResolvedCampaignStart):Promise<CampaignState>{
  if(!resolved.ok||!resolved.hexId)throw new Error(resolved.error||'Ponto inicial não resolvido.');
  const discovery=await loadMapDiscovery(campaign.id);
  const route=discovery.routeHexIds.includes(resolved.hexId)?discovery.routeHexIds:[...discovery.routeHexIds,resolved.hexId];
  await saveMapDiscovery({...discovery,currentHexId:resolved.hexId,discoveredHexIds:Array.from(new Set([...discovery.discoveredHexIds,resolved.hexId])),visitedHexIds:Array.from(new Set([...discovery.visitedHexIds,resolved.hexId])),routeHexIds:route,plannedRouteHexIds:[],updatedAt:Date.now()});
  const master=normalizeMasterState(campaign),locationName=resolved.venueName?resolved.venueName+' · '+resolved.locationName:resolved.locationName;
  return {...campaign,currentLocation:locationName,currentChapter:resolved.module?.title||(resolved.mode==='strider'?'Missão do Patrono':resolved.mode==='tavern'?'Prólogo na Taverna':campaign.currentChapter),campaignStart:resolved.selection,adventureRuntime:{moduleId:resolved.module?.id||(resolved.mode==='strider'?'strider-patron-quest':resolved.mode),status:'not_started',currentLocationName:locationName,currentHexId:resolved.hexId,patronId:resolved.selection.patronId,missionIndex:resolved.selection.missionIndex,missionSeed:resolved.missionSeed,discoveredClueIds:[],revealedSecretIds:[],completedObjectiveIds:[],failedObjectiveIds:[],triggeredEventIds:[],flags:{},history:[]},masterState:{...master,position:{...master.position,currentHexId:resolved.hexId,locationName,routeHexIds:route},scene:{...master.scene,locationName},updatedAt:Date.now()},updatedAt:Date.now()};
}

export function activateAdventureAfterOpening(campaign:CampaignState,sceneId:string,location:string):CampaignState{
  if(!campaign.adventureRuntime)return campaign;
  return {...campaign,adventureRuntime:{...campaign.adventureRuntime,status:'active',currentSceneId:sceneId,currentLocationName:location,currentHexId:normalizeMasterState(campaign).position.currentHexId||campaign.adventureRuntime.currentHexId,history:[...(campaign.adventureRuntime.history||[]),{sceneId,location,timestamp:Date.now()}]}};
}

export function recordAdventureScene(campaign:CampaignState,sceneId:string,location:string):CampaignState{
  const runtime=campaign.adventureRuntime;
  if(!runtime||runtime.status!=='active'||runtime.history.some(item=>item.sceneId===sceneId))return campaign;
  return {...campaign,adventureRuntime:{...runtime,currentSceneId:sceneId,currentLocationName:location,currentHexId:normalizeMasterState(campaign).position.currentHexId||runtime.currentHexId,history:[...runtime.history,{sceneId,location,timestamp:Date.now()}]}};
}

export function buildAdventureOpeningContext(campaign:CampaignState,resolved:ResolvedCampaignStart):string{
  return [
    '',
    '--- ADVENTURE ENGINE · ABERTURA CANÔNICA ---',
    'Modo inicial: '+campaignStartLabel(resolved.selection)+'.',
    'Local resolvido pelo host: '+(resolved.venueName?resolved.venueName+' · ':'')+resolved.locationName+'.',
    'Hex resolvido pelo Atlas: '+(resolved.hexId||'nenhum')+'.',
    resolved.module?'Aventura ativa: '+resolved.module.campaignTitle+' / '+resolved.module.title+'.':'',
    resolved.module?'Fonte: '+resolved.module.source.sourceBook+(resolved.module.source.printedPages?', páginas '+resolved.module.source.printedPages:'')+'.':'',
    resolved.missionSeed?'Missão do Patrono: '+resolved.missionSeed:'',
    'Fatos públicos de abertura:',
    ...resolved.openingFacts.map(fact=>'- '+fact),
    'REGRA: fatos GM-only, segredos e progressão futura não são conhecimento dos jogadores.',
    'REGRA: posição, aventura, Patrono e missão acima foram decididos pelo host. O Gemini narra; não substitui essas escolhas.',
    '--- FIM ADVENTURE ENGINE ---',
    '',
  ].filter(Boolean).join('\n');
}

export function buildAdventureRuntimeContext(campaign:CampaignState):string{
  const runtime=campaign.adventureRuntime;if(!runtime)return'';
  const module=ADVENTURE_MODULES[runtime.moduleId];
  return [
    '',
    '--- ADVENTURE ENGINE · ESTADO PERSISTENTE ---',
    'Módulo: '+runtime.moduleId+'.',
    'Status: '+runtime.status+'.',
    'Cena atual: '+(runtime.currentSceneId||'ainda não registrada')+'.',
    'Local atual: '+(runtime.currentLocationName||campaign.currentLocation)+'.',
    runtime.patronId?'Patrono: '+runtime.patronId+'.':'',
    runtime.missionSeed?'Missão ativa: '+runtime.missionSeed:'',
    module?'Aventura: '+module.title+'.':'',
    module?.campaignSequence?.length?'Sequência sugerida: '+module.campaignSequence.join(' → ')+'.':'',
    'Pistas descobertas: '+JSON.stringify(runtime.discoveredClueIds||[])+'.',
    'Segredos revelados: '+JSON.stringify(runtime.revealedSecretIds||[])+'.',
    'Objetivos concluídos: '+JSON.stringify(runtime.completedObjectiveIds||[])+'.',
    'Flags: '+JSON.stringify(runtime.flags||{})+'.',
    'A IA não altera este estado diretamente. Ela narra somente o estado já validado pelo host.',
    '--- FIM ADVENTURE ENGINE ---',
    '',
  ].filter(Boolean).join('\n');
}

export function startSummary(campaign:CampaignState):string{
  const start=campaign.campaignStart;
  if(start?.mode==='strider')return start.patronId?'Patrono: '+start.patronId+(Number.isInteger(start.missionIndex)?' · missão '+(Number(start.missionIndex)+1):''):'O Patrono e a missão serão resolvidos ao começar.';
  if(start?.mode==='tavern')return start.venueName?start.venueName+' · '+(start.resolvedLocationName||''):'A taverna será sorteada uma única vez ao começar.';
  if(start?.mode==='sandbox')return start.resolvedHexId?'Hex escolhido: '+start.resolvedHexId:'Escolha o hex inicial no Atlas.';
  if(start?.mode==='lone-lands')return'Primeira aventura: Toca de Troll, Se é Que Houve Uma… · Pônei Saltitante, Bri.';
  return'Marco do Livro Básico · sul de Ered Luin.';
}
