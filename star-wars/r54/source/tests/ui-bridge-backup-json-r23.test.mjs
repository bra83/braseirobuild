import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createEoteCoreCharacter, EOTE_CAREERS } from '../app/src/main/assets/vtt/js/rules/eoteCharacterCreation.mjs';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

function memoryStorage(initial = {}) {
  const map = new Map(Object.entries(initial));
  return { map, getItem(k){ return map.has(k) ? map.get(k) : null; }, setItem(k,v){ map.set(k,String(v)); }, removeItem(k){ map.delete(k); } };
}

function validCharacter(name='Rin') {
  const career=EOTE_CAREERS.smuggler;
  return createEoteCoreCharacter({
    name,
    speciesKey:'human', careerKey:'smuggler', specializationKey:'pilot',
    careerFreeSkills:career.skills.slice(0,4), specializationFreeSkills:career.specializations.pilot.skills.slice(0,2),
    speciesChoiceSkills:['atletismo','carisma'],
    motivation:'Proteger o esquadrão', obligationType:'Dívida', partySize:1,
  });
}

test('export de ficha usa JSON canônico e nome .json sem criar estado',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); const state=createCampaign('Ficha'); state.player.character=validCharacter('Rin Ás'); repo.save(state);
  const bridge=new BraseiroUiBridge({storage}); const result=bridge.exportCharacterJson();
  assert.equal(result.ok,true); assert.match(result.data.fileName,/\.json$/); assert.equal(JSON.parse(result.data.json).name,'Rin Ás');
});

test('import de ficha inválida não altera campanha nem cria snapshot',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); const state=createCampaign('Ficha'); state.player.character=validCharacter('Antes'); repo.save(state);
  const before=storage.getItem(repo.key); const bridge=new BraseiroUiBridge({storage}); const result=bridge.importCharacterJson('{ruim');
  assert.equal(result.ok,false); assert.equal(result.error,'CHARACTER_IMPORT_FAILED'); assert.equal(storage.getItem(repo.key),before); assert.equal(bridge.listSnapshots().data.length,0);
});

test('import de ficha válida cria snapshot antes da mutação e persiste nova ficha',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); const state=createCampaign('Ficha'); state.player.character=validCharacter('Antes'); repo.save(state);
  const bridge=new BraseiroUiBridge({storage}); const incoming=JSON.stringify(validCharacter('Depois')); const result=bridge.importCharacterJson(incoming);
  assert.equal(result.ok,true); assert.ok(result.data.snapshotId); assert.equal(repo.load().player.character.name,'Depois'); assert.equal(bridge.listSnapshots().data.length,1);
});

test('backup de campanha exporta JSON validado sem remover estado',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); repo.save(createCampaign('Operação Cinza')); const bridge=new BraseiroUiBridge({storage});
  const result=bridge.exportCampaignBackup(); assert.equal(result.ok,true); assert.match(result.data.fileName,/\.backup\.json$/); assert.equal(JSON.parse(result.data.json).name,'Operação Cinza'); assert.equal(repo.load().name,'Operação Cinza');
});

test('backup corrompido é rejeitado sem sobrescrever campanha nem snapshot',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); repo.save(createCampaign('Original')); const before=storage.getItem(repo.key); const bridge=new BraseiroUiBridge({storage});
  const result=bridge.importCampaignBackup('{quebrado'); assert.equal(result.ok,false); assert.equal(result.error,'BACKUP_IMPORT_FAILED'); assert.equal(storage.getItem(repo.key),before); assert.equal(bridge.listSnapshots().data.length,0);
});

test('restore de backup válido preserva snapshot automático do estado anterior',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); repo.save(createCampaign('Atual')); const bridge=new BraseiroUiBridge({storage});
  const other=createCampaign('Restaurada'); const result=bridge.importCampaignBackup(JSON.stringify(other)); assert.equal(result.ok,true); assert.ok(result.data.snapshotId); assert.equal(repo.load().name,'Restaurada'); const list=bridge.listSnapshots(); assert.equal(list.ok,true); assert.equal(list.data.length,1);
});

test('snapshot manual pode restaurar exatamente o estado salvo',()=>{
  const storage=memoryStorage(); const repo=new LocalStorageRepository(storage); const state=createCampaign('Snap A'); repo.save(state); const bridge=new BraseiroUiBridge({storage});
  const snap=bridge.createSnapshot('Antes'); assert.equal(snap.ok,true); const changed=repo.load(); changed.name='Snap B'; repo.save(changed); const restored=bridge.restoreSnapshot(snap.data.id); assert.equal(restored.ok,true); assert.equal(repo.load().name,'Snap A');
});

test('operações de backup/ficha sem campanha falham sem criar campanha padrão',()=>{
  const storage=memoryStorage(); const bridge=new BraseiroUiBridge({storage});
  assert.equal(bridge.exportCharacterJson().error,'NO_CAMPAIGN'); assert.equal(bridge.exportCampaignBackup().error,'NO_CAMPAIGN'); assert.equal(bridge.createSnapshot().error,'NO_CAMPAIGN'); assert.equal(storage.map.size,0);
});
