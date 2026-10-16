import test from 'node:test';
import assert from 'node:assert/strict';
import { validateCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createRandomAorCoreCharacter, AOR_SPECIES, AOR_CAREERS, DUTY_TYPES_PT_BR } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { BraseiroUiBridge, UI_BRIDGE_VERSION } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

function sequence(values){ let i=0; return ()=>values[(i++)%values.length]; }
function memoryStorage(initial={}){ const map=new Map(Object.entries(initial)); return {map,getItem:k=>map.has(k)?map.get(k):null,setItem:(k,v)=>map.set(k,String(v)),removeItem:k=>map.delete(k)}; }

test('gerador exige nome e motivação fornecidos pelo usuário',()=>{
  assert.throws(()=>createRandomAorCoreCharacter({motivation:'Dever',rng:()=>0}),/não inventa nomes/i);
  assert.throws(()=>createRandomAorCoreCharacter({name:'Rin',rng:()=>0}),/não inventa motivações/i);
});

test('gerador usa somente catálogos AoR auditados e produz ficha válida sem gastar PE aleatoriamente',()=>{
  const c=createRandomAorCoreCharacter({name:'Rin',motivation:'Servir à Aliança',rng:sequence([0.13,0.41,0.72,0.24,0.87,0.36,0.64,0.18,0.53,0.91])});
  assert.equal(validateCharacter(c),true);
  assert.ok(Object.values(AOR_SPECIES).some(s=>s.label===c.species));
  assert.ok(Object.values(AOR_CAREERS).some(x=>x.label===c.profession));
  assert.ok(DUTY_TYPES_PT_BR.includes(c.duties[0].type));
  assert.equal(c.motivations[0].text,'Servir à Aliança');
  assert.equal(c.name,'Rin');
  assert.equal(c.creation.randomizedMechanics,true);
  assert.equal(c.creation.characteristicXpSpent,0);
});

test('amostra múltiplas sequências sem ultrapassar grau 2 ou sair dos contratos de criação',()=>{
  for(let seed=1;seed<=80;seed++){
    let x=seed>>>0;
    const rng=()=>{ x=(1664525*x+1013904223)>>>0; return x/4294967296; };
    const c=createRandomAorCoreCharacter({name:`R${seed}`,motivation:'Dever',rng});
    assert.equal(validateCharacter(c),true);
    assert.ok(Math.max(...Object.values(c.skills))<=2);
    assert.equal(c.xp.available,c.xp.total);
  }
});

test('GameController constrói sem mutar e só persiste quando criação é confirmada',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); repo.save(createCampaign('Teste'));
  const game=new GameController(repo); const before=JSON.stringify(game.state);
  const built=game.buildRandomCharacter({name:'Rin',motivation:'Dever',rng:()=>0});
  assert.equal(validateCharacter(built),true); assert.equal(JSON.stringify(game.state),before);
  game.setCharacter(built); assert.equal(repo.load().player.character.name,'Rin');
});

test('ponte v3 cria ficha aleatória e snapshot somente quando substitui ficha existente',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); repo.save(createCampaign('Teste')); const bridge=new BraseiroUiBridge({storage});
  assert.equal(UI_BRIDGE_VERSION,3);
  const first=bridge.generateRandomCharacter({name:'Rin',motivation:'Dever',rng:()=>0});
  assert.equal(first.ok,true); assert.equal(first.data.snapshotId,null); assert.equal(repo.load().player.character.name,'Rin');
  const second=bridge.generateRandomCharacter({name:'Mara',motivation:'Serviço',rng:()=>0.5});
  assert.equal(second.ok,true); assert.ok(second.data.snapshotId); assert.equal(repo.load().player.character.name,'Mara'); assert.equal(bridge.listSnapshots().data.length,1);
});

test('falha de entrada não muta estado nem cria snapshot',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); repo.save(createCampaign('Teste')); const bridge=new BraseiroUiBridge({storage});
  const before=storage.getItem(repo.key); const result=bridge.generateRandomCharacter({name:'',motivation:'Dever',rng:()=>0});
  assert.equal(result.ok,false); assert.equal(result.error,'RANDOM_CHARACTER_FAILED'); assert.equal(storage.getItem(repo.key),before); assert.equal(bridge.listSnapshots().data.length,0);
});
