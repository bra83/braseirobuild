import test from 'node:test';
import assert from 'node:assert/strict';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';

function memoryStorage(){ const map=new Map(); return {getItem:k=>map.has(k)?map.get(k):null,setItem:(k,v)=>map.set(k,String(v)),removeItem:k=>map.delete(k)}; }
function setup(){ const storage=memoryStorage(); const repository=new LocalStorageRepository(storage); repository.save(createCampaign('Teste')); return {storage,repository,bridge:new BraseiroUiBridge({storage})}; }

const humanPilot={
  name:'Rin',speciesKey:'human',careerKey:'smuggler',specializationKey:'pilot',partySize:1,
  careerFreeSkills:['coordinacion','engano','bajos_fondos','percepcion'],
  specializationFreeSkills:['astronavegacion','artilleria'],
  speciesChoiceSkills:['carisma','negociacion'],
  characteristics:{fortaleza:2,agilidad:2,intelecto:2,astucia:2,voluntad:2,presencia:2},
  obligationType:'Dívida',obligationBenefits:{},motivation:'Servir à Aliança',background:'Teste'
};

test('bridge expõe catálogo manual somente a partir das tabelas EotE Core canônicas',()=>{
  const {bridge}=setup(); const result=bridge.creationCatalog(); assert.equal(result.ok,true);
  assert.equal(result.data.profile,'EOTE_CORE');
  assert.equal(result.data.species.length,8); assert.equal(result.data.careers.length,6); assert.equal(result.data.obligationTypes.length,12);
  assert.ok(result.data.species.every(x=>x.key&&x.label&&x.characteristics));
  assert.ok(result.data.careers.every(x=>x.specializations.length===3));
  assert.equal(result.data.skillLabels.medicina,'Medicina');
});

test('preview manual é não-mutante e calcula orçamento canônico',()=>{
  const {bridge,repository}=setup(); const before=JSON.stringify(repository.load());
  const result=bridge.creationPreview({speciesKey:'human',partySize:1,characteristics:humanPilot.characteristics,obligationBenefits:{}});
  assert.equal(result.ok,true); assert.equal(result.data.xpBudget,110); assert.equal(result.data.xpRemaining,110); assert.equal(result.data.credits,500);
  assert.equal(JSON.stringify(repository.load()),before);
});

test('criação manual válida persiste ficha e usa snapshot somente ao substituir',()=>{
  const {bridge,repository}=setup();
  const first=bridge.createCustomCharacter(humanPilot); assert.equal(first.ok,true); assert.equal(first.data.snapshotId,null); assert.equal(repository.load().player.character.name,'Rin');
  const second=bridge.createCustomCharacter({...humanPilot,name:'Mara',motivation:'Dever'}); assert.equal(second.ok,true); assert.ok(second.data.snapshotId); assert.equal(repository.load().player.character.name,'Mara');
  assert.equal(bridge.listSnapshots().data.length,1);
});

test('entrada manual inválida não cria snapshot nem muta campanha',()=>{
  const {bridge,repository}=setup(); bridge.createCustomCharacter(humanPilot); const before=JSON.stringify(repository.load()); const snapshotsBefore=bridge.listSnapshots().data.length;
  const result=bridge.createCustomCharacter({...humanPilot,careerFreeSkills:['atletismo']});
  assert.equal(result.ok,false); assert.equal(result.error,'CUSTOM_CHARACTER_FAILED'); assert.equal(JSON.stringify(repository.load()),before); assert.equal(bridge.listSnapshots().data.length,snapshotsBefore);
});
