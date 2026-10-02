import type { CampaignState, Character } from './db';
import { loadMapDiscovery, loadRuntimeMapManifest, loadRuntimeTerraMediaLocations, type EriadorHex, type TerraMediaLocation } from './map';
import { normalizeMasterState } from './master/state';
import { CAMPAIGN_START_OPTIONS, LONE_LANDS_CAMPAIGN, STAR_OF_MIST, STRIDER_PATRONS } from './adventureModules';
import { STRIDER_MISSIONS } from './striderPatronMissions';
import type { CampaignStartSelection, ResolvedCampaignStart } from './adventureTypes';

const norm=(value:unknown)=>String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
const firstHex=(location:TerraMediaLocation)=>location.hexId||location.hexIds?.[0]||null;
const locationText=(location:TerraMediaLocation)=>norm([location.name,location.nameOriginal,...(location.mapNames||[]),...(location.aliases||[]),location.type,...(location.categories||[]),...(location.regions||[]),...(location.tags||[])].join(' '));
function stableIndex(seed:string,size:number){if(size<=1)return 0;let hash=2166136261;for(const ch of seed){hash^=ch.charCodeAt(0);hash=Math.imul(hash,16777619);}return Math.abs(hash>>>0)%size;}

function scoreLocation(location:TerraMediaLocation,definition:{aliases:string[];regionAliases?:string[]}){
  const values=[location.name,location.nameOriginal,...(location.mapNames||[]),...(location.aliases||[])].map(norm).filter(Boolean);
  const whole=locationText(location);let score=0;
  for(const raw of definition.aliases){const alias=norm(raw);if(!alias)continue;if(values.includes(alias))score=Math.max(score,1000);else if(values.some(value=>value.includes(alias)||alias.includes(value)))score=Math.max(score,760);else if(whole.includes(alias))score=Math.max(score,540);}
  for(const raw of definition.regionAliases||[]){const alias=norm(raw);if(alias&&whole.includes(alias))score=Math.max(score,420);}
  if(location.placement?.status==='verified-label')score+=35;if(firstHex(location))score+=50;return score;
}
function scoreHex(hex:EriadorHex,definition:{aliases:string[];regionAliases?:string[];bias?:string}){
  if(!hex.playable||!hex.enabled)return -1;const text=norm([hex.name,hex.poi,hex.region,hex.terrain,...(hex.tags||[])].join(' '));let score=0;
  for(const raw of [...definition.aliases,...(definition.regionAliases||[])]){const alias=norm(raw);if(alias&&text.includes(alias))score=Math.max(score,300);}
  if(score>0&&definition.bias==='southeast')score+=Math.round((hex.normalizedCenterX||0)*40+(hex.normalizedCenterY||0)*40);return score;
}
function resolveDefinition(definition:any,locations:TerraMediaLocation[],hexes:EriadorHex[]){
  const ranked=locations.map(location=>({location,score:scoreLocation(location,definition)})).filter(item=>item.score>0&&firstHex(item.location)).sort((a,b)=>b.score-a.score||a.location.name.localeCompare(b.location.name,'pt-BR'));
  if(ranked.length)return {hexId:firstHex(ranked[0].location),locationName:definition.canonicalName,venueName:definition.venueName,source:'location-catalog'};
  const byHex=hexes.map(hex=>({hex,score:scoreHex(hex,definition)})).filter(item=>item.score>0).sort((a,b)=>b.score-a.score||a.hex.id.localeCompare(b.hex.id));
  if(byHex.length)return {hexId:byHex[0].hex.id,locationName:definition.canonicalName,venueName:definition.venueName,source:'map-region'};
  return {hexId:null,locationName:definition.canonicalName,venueName:definition.venueName,source:'unresolved'};
}
function patronId(value:string){const key=norm(value);if(key.includes('balin'))return'balin';if(key.includes('bilbo'))return'bilbo';if(key.includes('cirdan'))return'cirdan';if(key.includes('gandalf'))return'gandalf';if(key.includes('gilraen'))return'gilraen';if(key.includes('tom bombadil')||key.includes('fruta d ouro')||key.includes('goldberry'))return'tom';return'';}
function tavernCandidates(locations:TerraMediaLocation[]){
  const named=locations.filter(location=>firstHex(location)&&/taverna|taberna|estalagem|pousada|inn|tavern|prancing pony|ponei saltitante/.test(locationText(location)));
  return named.length?named:locations.filter(location=>firstHex(location)&&/assentamento|cidade|vila|aldeia|town|village|settlement|bri|bree|hobbiton|mithlond|rivendell|valfenda/.test(locationText(location)));
}

export function defaultCampaignStart():CampaignStartSelection{return{mode:'star-of-mist',adventureId:STAR_OF_MIST.id};}
export function normalizeCampaignStart(input?:CampaignState['campaignStart']|null):CampaignStartSelection{
  const raw=(input||{}) as CampaignStartSelection;const mode=CAMPAIGN_START_OPTIONS.some(option=>option.mode===raw.mode)?raw.mode:'star-of-mist';
  return{...raw,mode,adventureId:raw.adventureId||(mode==='star-of-mist'?STAR_OF_MIST.id:mode==='lone-lands'?LONE_LANDS_CAMPAIGN.id:undefined)};
}
export function campaignStartCanBegin(campaign:CampaignState){const selection=normalizeCampaignStart(campaign.campaignStart);return selection.mode!=='sandbox'||Boolean(normalizeMasterState(campaign).position.currentHexId);}
export function campaignStartLabel(input?:CampaignState['campaignStart']|null){const selection=normalizeCampaignStart(input);return CAMPAIGN_START_OPTIONS.find(option=>option.mode===selection.mode)?.label||selection.mode;}

export async function resolveCampaignStart(campaign:CampaignState,characters:Character[]):Promise<ResolvedCampaignStart>{
  const selection=normalizeCampaignStart(campaign.campaignStart);
  const [locations,manifest,discovery]=await Promise.all([loadRuntimeTerraMediaLocations().catch(()=>[]),loadRuntimeMapManifest().catch(()=>null),loadMapDiscovery(campaign.id).catch(()=>null)]);const hexes=manifest?.hexes||[];
  if(selection.mode==='sandbox'){
    const hexId=discovery?.currentHexId||normalizeMasterState(campaign).position.currentHexId||null;
    if(!hexId)return{ok:false,requiresManualAtlas:true,mode:selection.mode,selection,module:null,hexId:null,locationName:campaign.currentLocation||'Eriador',openingFacts:[],resolutionSource:'player-atlas-required',error:'Escolha no Atlas o hex inicial. No modo Sandbox, o sistema não escolhe por você.'};
    return{ok:true,requiresManualAtlas:false,mode:selection.mode,selection:{...selection,resolvedHexId:hexId,resolvedLocationName:campaign.currentLocation,resolutionSource:'player-atlas'},module:null,hexId,locationName:campaign.currentLocation||'Eriador',openingFacts:['Modo Sandbox: ponto inicial escolhido manualmente no Atlas.'],resolutionSource:'player-atlas'};
  }
  if(selection.mode==='tavern'){
    const candidates=tavernCandidates(locations);if(!candidates.length)return{ok:false,requiresManualAtlas:false,mode:selection.mode,selection,module:null,hexId:null,locationName:'Eriador',openingFacts:[],resolutionSource:'tavern-catalog-empty',error:'O Atlas não possui taverna ou assentamento mapeado utilizável.'};
    const chosen=candidates[stableIndex(campaign.id+':tavern-start',candidates.length)],named=/taverna|taberna|estalagem|pousada|inn|tavern|prancing pony|ponei saltitante/.test(locationText(chosen)),venue=named?chosen.name:'uma taverna em '+chosen.name,hexId=firstHex(chosen);
    return{ok:Boolean(hexId),requiresManualAtlas:false,mode:selection.mode,selection:{...selection,resolvedHexId:hexId||undefined,resolvedLocationName:chosen.name,venueName:venue,resolutionSource:named?'mapped-tavern':'mapped-settlement-tavern'},module:null,hexId,locationName:chosen.name,venueName:venue,openingFacts:['Modo Taverna: o host escolheu este local e fixou a Companhia no Atlas.','A primeira cena acontece nesta taverna; o Gemini não escolhe outra cidade nem move o grupo.'],resolutionSource:named?'mapped-tavern':'mapped-settlement-tavern'};
  }
  if(selection.mode==='strider'){
    const hero=characters.find(character=>character.sheet.other?.rulesMode==='strider')||characters[0],id=selection.patronId||patronId(hero?.sheet.identity.patron||'')||'gandalf',patron=STRIDER_PATRONS[id]||STRIDER_PATRONS.gandalf,missions=STRIDER_MISSIONS[patron.id]||STRIDER_MISSIONS.gandalf,missionIndex=Number.isInteger(selection.missionIndex)?Math.max(0,Math.min(5,Number(selection.missionIndex))):stableIndex(campaign.id+':'+patron.id,missions.length),missionSeed=missions[missionIndex],resolved=resolveDefinition(patron.start,locations,hexes);
    return{ok:Boolean(resolved.hexId),requiresManualAtlas:false,mode:selection.mode,selection:{...selection,patronId:patron.id,missionIndex,resolvedHexId:resolved.hexId||undefined,resolvedLocationName:resolved.locationName,venueName:resolved.venueName,resolutionSource:resolved.source},module:null,hexId:resolved.hexId,locationName:resolved.locationName,venueName:resolved.venueName,missionSeed,openingFacts:['Modo Peregrino: missão inicial derivada das tabelas de Patrono das páginas 5–7.','Patrono: '+patron.label+'.','Semente: '+missionSeed,'Questões em aberto permanecem desconhecidas até serem descobertas.'],resolutionSource:resolved.source,error:resolved.hexId?undefined:'Não foi possível resolver no Atlas um ponto canônico para o Patrono.'};
  }
  const module=selection.mode==='lone-lands'?LONE_LANDS_CAMPAIGN:STAR_OF_MIST,resolved=resolveDefinition(module.start,locations,hexes);
  return{ok:Boolean(resolved.hexId),requiresManualAtlas:false,mode:selection.mode,selection:{...selection,adventureId:module.id,resolvedHexId:resolved.hexId||undefined,resolvedLocationName:resolved.locationName,venueName:resolved.venueName,resolutionSource:resolved.source},module,hexId:resolved.hexId,locationName:resolved.locationName,venueName:resolved.venueName,openingFacts:[...module.openingFacts],resolutionSource:resolved.source,error:resolved.hexId?undefined:'Não foi possível localizar automaticamente o ponto inicial desta aventura no Atlas.'};
}
