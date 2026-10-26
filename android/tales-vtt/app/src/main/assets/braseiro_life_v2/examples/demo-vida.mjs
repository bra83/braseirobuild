import {createLegacyStore,createLifeStore,transact,installRecipes,buildSceneContext} from '../src/index.mjs';
import {saveFile,loadFile} from '../src/node-persistence.mjs';
import {tmpdir} from 'node:os';
import {mkdtemp,rm} from 'node:fs/promises';
import {join} from 'node:path';
let s=installRecipes(createLifeStore(createLegacyStore({system:'dnd5e',worldId:'demo'})),transact);
let n=0;const dispatch=c=>s=transact(s,{id:`demo-${++n}`,...c},{verifiedByHost:true});
dispatch({type:'fact',key:'supply-restored',value:false,visibility:'public',locationId:'village'});
dispatch({type:'advance',toDay:7,locationId:'village'});
console.log('DIA 7:',{alimentos:s.entities.food.quantity,preco:s.facts['food-price-factor'].value});
// The host confirms an unanticipated solution: a river delivery arranged by the party.
dispatch({type:'event',event:{id:'river-delivery',summary:'O grupo negociou entrega fluvial.',visibility:'public',actorIds:['hero'],witnessIds:['innkeeper']}});
dispatch({type:'fact',key:'supply-restored',value:true,visibility:'public',locationId:'village'});
dispatch({type:'evaluate',locationId:'village'});
console.log('DEPOIS DA SOLUÇÃO:',{alimentos:s.entities.food.quantity,preco:s.facts['food-price-factor'].value,problema:s.entities.supply.status});
dispatch({type:'fact',key:'returned-home',value:true,visibility:'private',actorIds:['hero'],locationId:'village'});
dispatch({type:'evaluate',locationId:'village'});
console.log('ESCOLHA DO JOGADOR:',buildSceneContext(s,{locationId:'village',actorIds:['hero']}).offers);
const dir=await mkdtemp(join(tmpdir(),'braseiro-demo-'));
try{const path=join(dir,'save.json');await saveFile(path,s);for(let i=0;i<3;i++)s=await loadFile(path);console.log('TRÊS REABERTURAS:',{dia:s.day,eventos:s.events.length,ofertasPendentes:Object.values(s.offers).filter(o=>o.status==='pending').length});}finally{await rm(dir,{recursive:true,force:true});}
