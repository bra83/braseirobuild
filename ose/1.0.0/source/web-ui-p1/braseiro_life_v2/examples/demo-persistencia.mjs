/** Demonstração de dois módulos, mesma ficha e mesmos fatos ao reabrir um arquivo.
 * NÃO é o adaptador Android. Cria apenas uma simulação segura em arquivo local. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {createLegacyStore,beginAdventure,applyVerifiedEvent,finishAdventure,assertLegacyStore,buildCharacterLegacyContext} from '../src/legacy-engine.mjs';
const original=JSON.parse(fs.readFileSync(new URL('a-maldicao-de-karvath.json',import.meta.url),'utf8'));
const savefile=path.join(fs.mkdtempSync(path.join(os.tmpdir(),'braseiro-legado-')),'legado.json');
let store=beginAdventure(createLegacyStore({system:original.system,worldId:original.worldId}),original,['garrick']);
const event={id:'dragao-derrubado-1',adventureId:original.id,system:original.system,worldId:original.worldId,timelineId:'principal',type:'creature_defeated',summary:'Garrick derrotou Karvath.',occurredAt:'1492-07-22',locationId:'covil-do-dragao',subject:{id:'dragao-karvath',name:'Karvath'},actors:[{characterId:'garrick',role:'autor-do-golpe-final'}],witnessIds:['prefeita-nara'],knownRegionIds:['vale-karvath']};
// Somente na integração real: esse sinal deve vir do motor de regras do VTT.
store=applyVerifiedEvent(store,event,{verifiedByHost:true});
store=finishAdventure(store,original.id,{endingSummary:'O vale mudou depois da derrota de Karvath.'});
fs.writeFileSync(savefile,JSON.stringify(store,null,2));
// Reabre do disco para simular o fechamento e a restauração do app.
store=assertLegacyStore(JSON.parse(fs.readFileSync(savefile,'utf8')));
const sequel={...original,id:'pousada-caminho-norte',title:'A Pousada do Caminho Norte'};
store=beginAdventure(store,sequel,['garrick']);
fs.writeFileSync(savefile,JSON.stringify(store,null,2));
console.log(JSON.stringify({savefile,adventures:Object.keys(store.adventures),legacy:buildCharacterLegacyContext(store,'garrick',{regionId:'vale-karvath'})},null,2));
