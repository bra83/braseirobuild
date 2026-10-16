
import { adversaryCompendiumSearch, adversaryProfileExists } from './adversaries.mjs';
export function masterAdversarySearch(query='',options={}){ return adversaryCompendiumSearch(query,options); }
export function masterEncounterDirective({profileKey,count=1,range='Médio',name=null,worldNpcId=null,factionId=null,sourceHookId=null}={}){
  if(!adversaryProfileExists(profileKey)) throw new Error(`Perfil de adversário não encontrado: ${profileKey}`);
  return {sourceHookId,adversaries:[{profileKey,count,range,name,worldNpcId,factionId}]};
}
export function masterEncounterDirectiveBatch(adversaries,{sourceHookId=null}={}){
  if(!Array.isArray(adversaries)||!adversaries.length) throw new Error('Lista de adversários vazia.');
  for(const a of adversaries) if(!adversaryProfileExists(a.profileKey)) throw new Error(`Perfil de adversário não encontrado: ${a.profileKey}`);
  return {sourceHookId,adversaries:adversaries.map(a=>({count:1,range:'Médio',name:null,worldNpcId:null,factionId:null,...a}))};
}
