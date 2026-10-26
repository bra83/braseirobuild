import {transact,buildSceneContext,assertLifeStore} from './life-engine.mjs';
import {planAutomaticMatrices} from './matrix-automation.mjs';
/** Trusted host facade. All effects, automatic draws and schedules commit atomically. */
export class LifeRuntime {
 constructor(repository,key,state,{matrixProvider}={}){this.repository=repository;this.key=key;this.state=assertLifeStore(state);this.queue=Promise.resolve();this.matrixProvider=matrixProvider;}
 static async open(repository,key,initial,options={}){let state=await repository.load(key);if(!state){await repository.save(key,initial,null);state=initial;}return new LifeRuntime(repository,key,state,options);}
 dispatch(command){const operation=this.queue.then(async()=>{
  const before=this.state;let next=transact(before,command,{verifiedByHost:true,expectedRevision:before.revision});
  if(next.revision===before.revision)return structuredClone(next);
  const plan=planAutomaticMatrices(next,command);
  if(plan.length){
   if(!this.matrixProvider)throw Error('Automação habilitada sem provedor de matrizes');
   const bundle=await this.matrixProvider(next.matrixProfile.packId);
   for(const item of plan){
    if(next.budget.used>=next.budget.max)break;
    next=transact(next,{id:item.id,type:'matrix_draw',bundle,context:item,occurrenceChance:item.occurrenceChance},{verifiedByHost:true});
    next=transact(next,{id:`schedule-${item.id}`,type:'matrix_schedule',characterId:item.characterId,nextDueDay:next.day+next.matrixAutomation.cooldownDays},{verifiedByHost:true});
    if(next.matrixAutomation.autoPresent){
     const offer=Object.values(next.offers).find(o=>o.source===item.id&&o.status==='pending');
     if(offer)next=transact(next,{id:`present-${item.id}`,type:'matrix_present',offerId:offer.id},{verifiedByHost:true});
    }
   }
  }
  await this.repository.save(this.key,next,before.revision);this.state=next;return structuredClone(next);
 });this.queue=operation.catch(()=>{});return operation;}
 snapshot(){return structuredClone(this.state);}
 scene(options){return buildSceneContext(this.state,options);}
 async reload(){await this.queue;const s=await this.repository.load(this.key);if(!s)throw Error('Save inexistente');this.state=assertLifeStore(s);return this.snapshot();}
}
