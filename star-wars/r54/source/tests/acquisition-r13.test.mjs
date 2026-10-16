import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { catalogWeaponToCharacterWeapon } from '../app/src/main/assets/vtt/js/rules/acquisition.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('pedido de aquisição fica pendente e persiste sem gastar créditos',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'acq-pending')); game.loadStarterCharacter('jin-rio');
  const before=game.state.player.character.credits; const r=game.proposeWeaponAcquisition('blaster_pistol');
  assert.equal(game.state.player.character.credits,before); assert.equal(r.pendingAcquisition.itemId,'blaster_pistol'); assert.equal(game.state.master.pendingAcquisition.status,'aguardando_mestre');
  const reopened=new GameController(new LocalStorageRepository(storage,'acq-pending')); assert.equal(reopened.state.master.pendingAcquisition.itemName,'Pistola blaster');
});

test('não permite abrir um segundo pedido enquanto o Mestre não resolver o primeiro',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'acq-lock')); game.loadStarterCharacter('jin-rio'); game.proposeWeaponAcquisition('blaster_pistol');
  assert.throws(()=>game.proposeWeaponAcquisition('pocket_blaster'),/já existe uma aquisição/i);
});

test('decisão do Mestre sem regra recuperada não pode concluir aquisição',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'acq-ground')); game.loadStarterCharacter('jin-rio'); game.proposeWeaponAcquisition('blaster_pistol');
  const before=structuredClone(game.state);
  assert.throws(()=>game.applyMasterProposal({acquisitionDecision:{itemId:'blaster_pistol',approved:true,finalPrice:400}}),/exige referência a regra recuperada/i);
  assert.equal(game.state.player.character.credits,before.player.character.credits); assert.equal(game.state.master.pendingAcquisition.itemId,'blaster_pistol');
});

test('decisão estruturada e ancorada conclui a compra exatamente uma vez',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'acq-ok')); game.loadStarterCharacter('jin-rio'); game.proposeWeaponAcquisition('blaster_pistol');
  game.lastMasterContext={rules:[{id:'SW-AOR-GEAR-SCREEN-001'}]}; const before=game.state.player.character.credits;
  const proposal=game.applyMasterProposal({narration:'A arma está disponível.',acquisitionDecision:{itemId:'blaster_pistol',approved:true,finalPrice:400,reason:'Disponibilidade confirmada pelo Mestre.'},sourceRuleIds:['SW-AOR-GEAR-SCREEN-001']});
  assert.equal(proposal.acquisitionDecision.approved,true); assert.equal(game.state.player.character.credits,before-400); assert.equal(game.state.master.pendingAcquisition,null);
  assert.equal(game.state.player.character.weapons.at(-1).sourceCatalogId,'blaster_pistol'); assert.equal(game.state.master.acquisitionHistory.at(-1).status,'adquirida');
  assert.throws(()=>game.applyMasterProposal({acquisitionDecision:{itemId:'blaster_pistol',approved:true,finalPrice:400},sourceRuleIds:['SW-AOR-GEAR-SCREEN-001']}),/não há aquisição pendente/i);
  const reopened=new GameController(new LocalStorageRepository(storage,'acq-ok')); assert.equal(reopened.state.player.character.credits,before-400); assert.equal(reopened.state.master.acquisitionHistory.length,1);
});

test('créditos insuficientes revertem toda a proposta e preservam o pedido',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'acq-poor')); game.loadStarterCharacter('cael'); game.proposeWeaponAcquisition('blaster_pistol'); game.lastMasterContext={rules:[{id:'SW-AOR-GEAR-SCREEN-001'}]};
  const narration=game.state.scene.narration, before=game.state.player.character.credits;
  assert.throws(()=>game.applyMasterProposal({narration:'Compra autorizada.',acquisitionDecision:{itemId:'blaster_pistol',approved:true,finalPrice:400},sourceRuleIds:['SW-AOR-GEAR-SCREEN-001']}),/Créditos insuficientes/i);
  assert.equal(game.state.player.character.credits,before); assert.equal(game.state.scene.narration,narration); assert.equal(game.state.master.pendingAcquisition.itemId,'blaster_pistol');
});

test('rejeição do Mestre encerra o pedido sem alterar créditos ou inventário',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'acq-no')); game.loadStarterCharacter('jin-rio'); const beforeW=game.state.player.character.weapons.length,beforeC=game.state.player.character.credits; game.proposeWeaponAcquisition('blaster_pistol'); game.lastMasterContext={rules:[{id:'SW-AOR-GEAR-SCREEN-001'}]};
  game.applyMasterProposal({acquisitionDecision:{itemId:'blaster_pistol',approved:false,reason:'Indisponível.'},sourceRuleIds:['SW-AOR-GEAR-SCREEN-001']});
  assert.equal(game.state.player.character.credits,beforeC); assert.equal(game.state.player.character.weapons.length,beforeW); assert.equal(game.state.master.acquisitionHistory.at(-1).status,'rejeitada');
});

test('conversão do catálogo calcula dano por Fortaleza e propriedades já suportadas',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'acq-map')); game.loadStarterCharacter('cael'); const c=game.state.player.character;
  const knife=catalogWeaponToCharacterWeapon('combat_knife',c); assert.equal(knife.damage,c.characteristics.fortaleza+1);
  const mine=catalogWeaponToCharacterWeapon('anti_personnel_mine',c); assert.equal(mine.blast,4);
  const stun=catalogWeaponToCharacterWeapon('stun_grenade',c); assert.equal(stun.damageType,'strain'); assert.equal(stun.blast,8);
});
