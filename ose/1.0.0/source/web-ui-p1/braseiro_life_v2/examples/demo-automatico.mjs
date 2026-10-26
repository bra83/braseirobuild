import {createLegacyStore,createLifeStore,LifeRuntime} from '../src/index.mjs';
import {loadMatrixBundle} from '../src/node-matrices.mjs';
import {saveFile,loadFile} from '../src/node-persistence.mjs';
import {mkdtemp,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
const dir=await mkdtemp(join(tmpdir(),'braseiro-auto-'));
try{
 const path=join(dir,'campaign.json');const repo={load:()=>loadFile(path),save:(key,state,revision)=>saveFile(path,state,revision)};
 const legacy=createLegacyStore({system:'dnd5e',worldId:'demo'});legacy.characters.hero={characterId:'hero',events:[]};
 let rt=await LifeRuntime.open(repo,'campaign',createLifeStore(legacy),{matrixProvider:loadMatrixBundle});
 await rt.dispatch({id:'profile',type:'matrix_configure',packId:'dnd5e'});
 await rt.dispatch({id:'person',type:'matrix_character_profile',profile:{characterId:'hero',capabilities:['record_available','related_person','support_available'],domainWeights:{justice:3},occurrenceChance:100}});
 await rt.dispatch({id:'capture',type:'event',event:{id:'capture',summary:'O grupo capturou o suspeito.',lifeCauseId:'cause-justice-1',actorIds:['hero'],visibility:'private',locationId:'village'}});
 await rt.dispatch({id:'enable',type:'matrix_automation_configure',options:{enabled:true,characterIds:['hero']}});
 // No matrix_draw command: an ordinary rest triggers the complete pipeline.
 await rt.dispatch({id:'rest',type:'rest',toDay:10,locationId:'village',actorIds:['hero']});
 console.log('Sorteios após descanso:',rt.snapshot().matrixHistory.length);
 console.log('Cena:',rt.scene({actorIds:['hero'],locationId:'village'}).offers);
 const arc=Object.values(rt.snapshot().lifeArcs)[0];console.log('Arco apresentado:',arc.status);
 await rt.dispatch({id:'support',type:'event',event:{id:'support',summary:'Ajuda prática recebida.',actorIds:['hero'],visibility:'private',lifeArcSignals:[{arcId:arc.id,outcome:'support_received'}]}});
 console.log('Após apoio confirmado:',rt.snapshot().lifeArcs[arc.id].status);
 for(let i=0;i<3;i++)rt=await LifeRuntime.open(repo,'campaign',null,{matrixProvider:loadMatrixBundle});
 console.log('Após três reaberturas:',{day:rt.snapshot().day,arc:rt.snapshot().lifeArcs[arc.id].status,automation:rt.snapshot().matrixAutomation.enabled});
}finally{await rm(dir,{recursive:true,force:true});}
