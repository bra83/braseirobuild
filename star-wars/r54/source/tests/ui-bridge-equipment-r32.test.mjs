import test from 'node:test';
import assert from 'node:assert/strict';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';

function memoryStorage(){
  const map=new Map();
  return {getItem(k){return map.has(k)?map.get(k):null},setItem(k,v){map.set(k,String(v))},removeItem(k){map.delete(k)}};
}
function bridgeWithCharacter(){
  const storage=memoryStorage();
  const repository=new LocalStorageRepository(storage);
  const game=new GameController(repository);
  game.loadStarterCharacter('zal');
  return {bridge:new BraseiroUiBridge({storage}),repository};
}

test('bridge expõe somente o catálogo canônico do controller',()=>{
  const {bridge}=bridgeWithCharacter();
  const result=bridge.equipmentCatalog();
  assert.equal(result.ok,true);
  assert.ok(Array.isArray(result.data.weapons));
  assert.ok(result.data.weapons.length>0);
  assert.ok(Array.isArray(result.data.armor));
  assert.ok(Array.isArray(result.data.generalGear));
});

test('proposta pelo bridge cria pendingAcquisition sem compra direta',()=>{
  const {bridge}=bridgeWithCharacter();
  const catalog=bridge.equipmentCatalog();
  const item=catalog.data.weapons[0];
  const before=bridge.readState().data.player.character;
  const result=bridge.proposeWeaponAcquisition(item.id);
  assert.equal(result.ok,true);
  assert.equal(result.data.pendingAcquisition.itemId,item.id);
  assert.equal(result.data.pendingAcquisition.status,'aguardando_mestre');
  assert.equal(result.data.state.player.character.credits,before.credits);
  assert.equal(result.data.state.player.character.weapons.length,before.weapons.length);
});

test('bridge recusa aquisição desconhecida e segundo pedido concorrente',()=>{
  const {bridge}=bridgeWithCharacter();
  assert.equal(bridge.proposeWeaponAcquisition('nao-existe').ok,false);
  const item=bridge.equipmentCatalog().data.weapons[0];
  assert.equal(bridge.proposeWeaponAcquisition(item.id).ok,true);
  const second=bridge.proposeWeaponAcquisition(item.id);
  assert.equal(second.ok,false);
  assert.match(second.message,/aquisição aguardando/i);
});
