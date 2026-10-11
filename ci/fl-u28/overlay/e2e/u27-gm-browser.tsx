import React from 'react';
import {createRoot} from 'react-dom/client';
import {getDb} from '../src/db/db';
import {MagicCastingService} from '../src/services/MagicCastingService';
import {MagicGmPendingPanel} from '../src/components/MagicGmPendingPanel';
import {ForbiddenLandsMagicCatalogService} from '../src/services/ForbiddenLandsMagicCatalogService';
import {CampaignService} from '../src/services/CampaignService';
import {WorldStateService} from '../src/services/WorldStateService';
import {ModelRouterService} from '../src/services/ModelRouterService';
const campaignId='u27-browser-gm';
const ids=['path-of-truth','remote-vision','visions-of-past','divination','intuition','mountain-voice','speak-dead'];
const chosen=new URL(location.href).searchParams.get('spell')||ids[0];
const db=await getDb();
const campaign=await db.get('campaign',campaignId)||{...CampaignService.create(),id:campaignId,partyCharacterIds:[]};
await db.put('campaign',campaign);
await db.put('app_meta',{id:'activeCampaignId',value:campaignId,updatedAt:Date.now()});
if(!await db.get('world_state',campaignId))await db.put('world_state',WorldStateService.empty(campaignId));
const attributes=Object.fromEntries(['strength','agility','wits','empathy'].map(k=>[k,{current:3,max:3}]));
for(const spellId of ids){
  const characterId='u27-browser-'+spellId;
  if(await db.get('characters',characterId))continue;
  const spell=ForbiddenLandsMagicCatalogService.get(spellId)!;
  const talent=spell.discipline==='Consciência'?'Caminho da Visão':spell.discipline==='Canção da Pedra'?'Caminho das Rochas':'Caminho da Morte';
  const character:any={id:characterId,campaignId,name:'Runa',status:'alive',attributes,skills:{},talents:{[talent]:3},
    willpowerPoints:5,conditions:[],criticalInjuries:[],gear:[],weapons:[],armor:[],coins:{copper:0,silver:0,gold:0}};
  await db.put('characters',character);campaign.partyCharacterIds.push(character.id);await db.put('campaign',campaign);
  await MagicCastingService.castCharacter({characterId,spellId,spellName:spell.name,spellRank:spell.rank,
    talentName:talent,ritual:spell.ritual,willpowerSpent:1,rng:()=>.999,
    effectContext:{ritual:spell.ritual,distance:'long',pastAge:'day',terrain:'mountain',knowsName:true,nearDeathSite:true}});
}
// Explicit canned TRANSPORT, not real Gemini. Production request builder,
// rendering, receipt transaction and actual browser IndexedDB stay in use.
ModelRouterService.callModel=async()=>({answer:'O caminho conhecido recomenda cautela. Não há fatos registrados suficientes para afirmar o que existe além dele.',
  __modelId:'U27_BROWSER_TEST_TRANSPORT',worldOperations:[{type:'damage',amount:999}]});
createRoot(document.getElementById('root')!).render(<main><h1>U27 — resposta mágica persistente</h1>
  <MagicGmPendingPanel campaignId={campaignId} characterId={'u27-browser-'+chosen} refreshKey={0}/></main>);
(window as any).u27Ready=true;
