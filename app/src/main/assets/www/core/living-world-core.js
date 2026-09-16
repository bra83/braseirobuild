/**
 * Camada de Mundo Vivo do produto Braseiro WWN.
 *
 * Este módulo NÃO resolve regra WWN e NÃO inventa estatísticas de Facção.
 * Ele mantém apenas contexto procedural persistente para o Mestre: foco de NPC,
 * facção em evidência, pressão de encontro, clima de apresentação e pulsos de mundo.
 * Todos os efeitos são explicitamente não mecânicos até que um Rules/World service
 * com evidência WWN os transforme em contrato mecânico.
 */

const WEATHER = Object.freeze([
  Object.freeze({id:'CLEAR_COLD',label:'CÉU CLARO E FRIO'}),
  Object.freeze({id:'HIGH_CLOUDS',label:'NUVENS ALTAS'}),
  Object.freeze({id:'COLD_WIND',label:'VENTO FRIO'}),
  Object.freeze({id:'LIGHT_RAIN',label:'CHUVA LEVE'}),
  Object.freeze({id:'MIST',label:'NÉVOA'}),
  Object.freeze({id:'HEAVY_CLOUDS',label:'CÉU CARREGADO'}),
  Object.freeze({id:'DISTANT_STORM',label:'TEMPESTADE DISTANTE'}),
]);
const DAYPARTS=Object.freeze(['AMANHECER','MANHÃ','MEIO-DIA','TARDE','CREPÚSCULO','NOITE','MADRUGADA']);

function clone(v){return structuredClone(v)}
function hash32(text){let h=0x811c9dc5;for(const ch of String(text??'')){h^=ch.codePointAt(0);h=Math.imul(h,0x01000193)>>>0;}return h>>>0}
function choose(list,key){if(!Array.isArray(list)||!list.length)return null;return clone(list[hash32(key)%list.length])}
function idOf(v){return v?.id??v?.npcId??v?.name??String(v??'')}
function publicFactionPool(state){return state?.contentState?.public?.factions??[]}
function gmFactionPool(state){return state?.contentState?.gm?.factions??[]}
function encounterPool(state){return state?.contentState?.gm?.encounter_seeds??[]}
function rumorPool(state){return state?.contentState?.gm?.rumors??state?.contentState?.public?.rumors??[]}
function npcPool(state){return Object.values(state?.npcState??state?.worldState?.npcs??{})}

export function makeLivingWorldInitialState(state,{seed=null}={}){
  const master=seed??state?.rngState?.masterSeed??state?.campaignId??'wwn';
  const faction=choose(gmFactionPool(state),`${master}/lw/faction/0`);
  const npc=choose(npcPool(state),`${master}/lw/npc/0`);
  const encounter=choose(encounterPool(state),`${master}/lw/encounter/0`);
  const rumor=choose(rumorPool(state),`${master}/lw/rumor/0`);
  const weather=choose(WEATHER,`${master}/lw/weather/0`);
  return Object.freeze({
    version:1, authority:'BRASEIRO_CAMPAIGN_PROCEDURAL', mechanicalEffect:false,
    beat:0, day:0, dayPart:choose(DAYPARTS,`${master}/lw/daypart/0`)??'MANHÃ', weather,
    lastTrigger:'ADVENTURE_CREATED', lastRevision:Number(state?.revision??0),
    pulseHistory:Object.freeze([]),
    privateState:Object.freeze({spotlightNpcId:idOf(npc)||null,spotlightFactionId:idOf(faction)||null,pressureEncounter:encounter??null,activeRumor:rumor??null}),
  });
}

export function advanceLivingWorldPresentation(state,{trigger='PLAYER_ACTION',revision=null,elapsedDaysDelta=0}={}){
  if(!state||typeof state!=='object')throw new TypeError('state required');
  const master=state?.rngState?.masterSeed??state?.campaignId??'wwn';
  const current=state?.worldState?.livingWorld??makeLivingWorldInitialState(state,{seed:master});
  const beat=Number(current.beat??0)+1;
  const rev=Number.isInteger(revision)?revision:Number(state?.revision??0);
  const currentDay=Number(current.day??0);
  const canonicalElapsed=Number(state?.worldState?.elapsedDays??0);
  const day=Math.max(0,canonicalElapsed,currentDay+Number(elapsedDaysDelta||0));
  const key=`${master}/lw/${day}/${beat}/${rev}/${trigger}`;
  const faction=choose(gmFactionPool(state),`${key}/faction`);
  const npc=choose(npcPool(state),`${key}/npc`);
  const encounter=choose(encounterPool(state),`${key}/encounter`);
  const rumor=choose(rumorPool(state),`${key}/rumor`);
  const weather=choose(WEATHER,`${key}/weather`);
  const dayPart=DAYPARTS[(Math.max(0,DAYPARTS.indexOf(current.dayPart))+1)%DAYPARTS.length]??choose(DAYPARTS,`${key}/daypart`);
  const pulse=Object.freeze({
    beat,day,revision:rev,trigger,
    npcId:idOf(npc)||null,
    npcIntent:npc?.currentIntent??null,
    factionId:idOf(faction)||null,
    factionGoal:faction?.goal??null,
    factionConflict:faction?.conflict??null,
    encounterPressure:encounter??null,
    rumor:rumor??null,
    weather:weather?.label??null,
    authority:'BRASEIRO_CAMPAIGN_PROCEDURAL',mechanicalEffect:false,
  });
  return Object.freeze({
    ...clone(current),beat,day,dayPart,weather,
    lastTrigger:trigger,lastRevision:rev,
    pulseHistory:Object.freeze([...(current.pulseHistory??[]).slice(-11),Object.freeze({beat,day,revision:rev,trigger,weather:weather?.label??null,authority:'BRASEIRO_CAMPAIGN_PROCEDURAL',mechanicalEffect:false})]),
    privateState:Object.freeze({spotlightNpcId:pulse.npcId,spotlightFactionId:pulse.factionId,pressureEncounter:pulse.encounterPressure,activeRumor:pulse.rumor,npcIntent:pulse.npcIntent,factionGoal:pulse.factionGoal,factionConflict:pulse.factionConflict}),
  });
}

export function projectLivingWorldForMaster(state){
  const living=state?.worldState?.livingWorld;
  if(!living)return Object.freeze({status:'NO_LIVING_WORLD'});
  return Object.freeze({status:'OK',livingWorld:clone(living)});
}
