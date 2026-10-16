import test from 'node:test';
import assert from 'node:assert/strict';
import {AdventureRuntime,assertPersistable} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {getPublishedAdventureAdapter,publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule,publishedAdventureUaeSnapshot,validatePublishedAdventureRuntimeState} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';

const clone=structuredClone;
function malicious(){
  const getter={};Object.defineProperty(getter,'secret',{enumerable:true,get(){return 'hidden';}});
  const hidden={safe:1};Object.defineProperty(hidden,'silent',{value:2,enumerable:false});
  const serializer={safe:1};Object.defineProperty(serializer,'toJSON',{value(){return {safe:999};},enumerable:false});
  const symbol={safe:1};symbol[Symbol('hidden')]='not persisted';
  const array=[1,2];array.extra='discarded';
  return {getter,hidden,serializer,symbol,array};
}
for(const [name,value] of Object.entries(malicious())){
  test(`P135 JSON gate rejeita ${name} em payload e flags sem mutação parcial`,()=>{
    const adapter=getPublishedAdventureAdapter('under-black-sun');
    const runtime=new AdventureRuntime(publishedAdventureRuntimeModule(adapter.meta));
    const before=runtime.getState();
    assert.throws(()=>assertPersistable(value));
    assert.throws(()=>runtime.dispatchEvent('TEST',{value}));
    assert.deepEqual(runtime.getState(),before);
    assert.throws(()=>runtime.applyEffects([{type:'set-flag',flag:'unsafe',value}]));
    assert.deepEqual(runtime.getState(),before);
  });
}
for(const {id} of publishedAdventureCatalog()){
  test(`P135 ${id}: comparação com contador/relógio inexistente falha fechada`,()=>{
    const adapter=getPublishedAdventureAdapter(id), module=publishedAdventureRuntimeModule(adapter.meta);
    const runtime=new AdventureRuntime(module);
    for(const op of ['<=','<','>=','>']){
      assert.equal(runtime.isConditionMet(`clock:missing:${op}:5`),false);
      assert.equal(runtime.isConditionMet(`counter:missing:${op}:5`),false);
    }
    runtime.applyEffects([{type:'set-counter',counter:'threat',value:2},{type:'advance-clock',clockId:'alertClock',delta:2}]);
    assert.equal(runtime.isConditionMet('counter:threat:>=:2'),true);
    assert.equal(runtime.isConditionMet('counter:threat:<=:1'),false);
    assert.equal(runtime.isConditionMet('clock:alertClock:>=:2'),true);
    assert.equal(runtime.isConditionMet('clock:alertClock:<=:1'),false);
    assert.equal(runtime.isConditionMet('clock:alertClock:>=:invalid'),false);
    assert.equal(runtime.isConditionMet('counter:threat:<=:invalid'),false);
    const saved=JSON.parse(runtime.serialize());
    assert.deepEqual(AdventureRuntime.restore(module,saved).getState(),runtime.getState());
    assert.equal(validatePublishedAdventureRuntimeState(module,saved),true);
    const world=adapter.start({}).world;
    assert.deepEqual(publishedAdventureUaeSnapshot(JSON.parse(JSON.stringify(world)),adapter),world.publishedAdventureRuntime);
  });
  test(`P135 ${id}: importação adulterada rejeitada antes da migração`,()=>{
    const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta);
    const state=new AdventureRuntime(module).getState();
    const polluted=JSON.parse(JSON.stringify(state));
    polluted.flags=JSON.parse('{"__proto__":{"gm":true}}');
    assert.throws(()=>AdventureRuntime.restore(module,polluted),/Chave persistida inválid/);
    assert.equal({}.gm,undefined);
    const nonJson=new AdventureRuntime(module).getState();
    nonJson.flags.exploit=malicious().serializer;
    assert.throws(()=>AdventureRuntime.restore(module,nonJson),/não persistível/);
  });
}
