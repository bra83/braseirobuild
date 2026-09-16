import { DeterministicRng } from './rng-core.js';
import { makeStarterCampaignState } from './starter-bootstrap.js';
import { makeLivingWorldInitialState } from './living-world-core.js';
function clone(v){return structuredClone(v)}
function shuffle(list,rng,stream){const a=clone(list??[]);for(let i=a.length-1;i>0;i--){const j=rng.die(i+1,stream).result-1;[a[i],a[j]]=[a[j],a[i]];}return a}
function first(a){return a?.length?a[0]:null}
function idOf(v){return v?.id??v?.npcId??v?.name??(typeof v==='string'?v:null)}
const WEATHER=Object.freeze(['CÉU CLARO E FRIO','NUVENS ALTAS','VENTO FRIO','CHUVA LEVE','NÉVOA','CÉU CARREGADO','TEMPESTADE DISTANTE']);
export function secureAdventureSeed(){if(!globalThis.crypto?.getRandomValues)throw new Error('ENTROPIA_SEGURA_INDISPONIVEL');const a=new Uint32Array(8);globalThis.crypto.getRandomValues(a);return [...a].map(v=>v.toString(16).padStart(8,'0')).join('')}
export function makeProceduralAdventureState({character,pack,campaignId,displayName='AVENTURA WWN',masterSeed}={}){
  if(!masterSeed)throw new Error('masterSeed required');const base=clone(makeStarterCampaignState({character,pack,campaignId,displayName,masterSeed}));const rng=new DeterministicRng(masterSeed);
  const pub=base.contentState?.public??{};const gm=base.contentState?.gm??{};
  pub.startingHooks=shuffle(pub.startingHooks,rng,'adventure/hooks');pub.majorSites=shuffle(pub.majorSites,rng,'adventure/sites');pub.minorPois=shuffle(pub.minorPois,rng,'adventure/pois');pub.rumors=shuffle(pub.rumors,rng,'adventure/rumors');pub.factions=shuffle(pub.factions,rng,'adventure/factions');
  if(pub.settlement?.activeProblems)pub.settlement.activeProblems=shuffle(pub.settlement.activeProblems,rng,'adventure/problems');
  gm.starting_hooks=shuffle(gm.starting_hooks,rng,'adventure/gm-hooks');gm.major_sites=shuffle(gm.major_sites,rng,'adventure/gm-sites');gm.minor_pois=shuffle(gm.minor_pois,rng,'adventure/gm-pois');gm.rumors=shuffle(gm.rumors,rng,'adventure/gm-rumors');gm.encounter_seeds=shuffle(gm.encounter_seeds,rng,'adventure/encounters');gm.npcs=shuffle(gm.npcs,rng,'adventure/npcs');gm.factions=shuffle(gm.factions,rng,'adventure/gm-factions');
  if(gm.settlement?.active_problems)gm.settlement.active_problems=shuffle(gm.settlement.active_problems,rng,'adventure/gm-problems');
  const opening={
    hook:first(pub.startingHooks),site:first(pub.majorSites),poi:first(pub.minorPois),rumor:first(pub.rumors),faction:first(gm.factions),npc:first(gm.npcs),encounter:first(gm.encounter_seeds),problem:first(gm.settlement?.active_problems),
  };
  const weather=WEATHER[rng.die(WEATHER.length,'adventure/weather').result-1];
  const timeOfDay=['AMANHECER','MANHÃ','MEIO-DIA','TARDE','CREPÚSCULO','NOITE','MADRUGADA'][rng.die(7,'adventure/time').result-1];
  base.worldState={...(base.worldState??{}),
    adventureVariation:{version:2,seedFingerprint:masterSeed.slice(0,16),featuredHookId:idOf(opening.hook),featuredSiteId:idOf(opening.site),featuredPoiId:idOf(opening.poi),featuredRumorId:idOf(opening.rumor),featuredFactionId:idOf(opening.faction),featuredNpcId:idOf(opening.npc),featuredProblem:opening.problem??null,featuredEncounter:opening.encounter??null,encounterDeck:(gm.encounter_seeds??[]).map(idOf),npcDeck:(gm.npcs??[]).map(idOf),poiDeck:(gm.minor_pois??[]).map(idOf),factionDeck:(gm.factions??[]).map(idOf),rumorDeck:(gm.rumors??[]).map(idOf)},
    openingScene:{authority:'BRASEIRO_CAMPAIGN_PROCEDURAL',mechanicalEffect:false,...clone(opening),weather,timeOfDay},
    timeOfDay,weather:{id:'PROCEDURAL_OPENING_WEATHER',label:weather,authority:'BRASEIRO_CAMPAIGN_PROCEDURAL',mechanicalEffect:false},
  };
  base.worldState.livingWorld=makeLivingWorldInitialState(base,{seed:masterSeed});
  base.rngState=rng.exportState();return base;
}
