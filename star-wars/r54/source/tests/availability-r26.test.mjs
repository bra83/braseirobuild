import test from 'node:test';
import assert from 'node:assert/strict';
import { rarityDifficulty, effectiveRarity, availabilityProfile, rarityModifierReferenceById } from '../app/src/main/assets/vtt/js/rules/availability.mjs';
import { createPendingWeaponAcquisition, weaponAcquisitionAvailability, setPendingWeaponAcquisitionRarityModifier } from '../app/src/main/assets/vtt/js/rules/acquisition.mjs';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('tabela de raridade converte 0-10 para as dificuldades oficiais',()=>{
  const expected=new Map([[0,0],[1,0],[2,1],[3,1],[4,2],[5,2],[6,3],[7,3],[8,4],[9,4],[10,5]]);
  for(const [rarity,difficulty] of expected){
    const r=rarityDifficulty(rarity);
    assert.equal(r.baseDifficulty,difficulty);
    assert.equal(r.challenge,0);
  }
});

test('raridade acima de 10 mantém Formidável e melhora a dificuldade uma vez por ponto',()=>{
  const r11=rarityDifficulty(11); assert.deepEqual({difficulty:r11.difficulty,challenge:r11.challenge,upgrades:r11.upgrades},{difficulty:4,challenge:1,upgrades:1});
  const r15=rarityDifficulty(15); assert.deepEqual({difficulty:r15.difficulty,challenge:r15.challenge,upgrades:r15.upgrades},{difficulty:0,challenge:5,upgrades:5});
  const r16=rarityDifficulty(16); assert.deepEqual({difficulty:r16.difficulty,challenge:r16.challenge,upgrades:r16.upgrades},{difficulty:1,challenge:5,upgrades:6});
});

test('modificador explícito altera raridade, nunca preço',()=>{
  assert.equal(effectiveRarity(4,-2),2);
  assert.equal(effectiveRarity(1,-4),0);
  const p=availabilityProfile({rarity:4,restricted:false,rarityModifier:-2});
  assert.equal(p.effectiveRarity,2); assert.equal(p.difficulty,1); assert.equal(p.skillKey,'negociacion');
  assert.equal('price' in p,false);
});

test('item legal usa Negociação e permite alternativa de Conhecimento a critério do Mestre',()=>{
  const p=availabilityProfile({rarity:5,restricted:false,rarityModifier:0});
  assert.equal(p.skillKey,'negociacion'); assert.equal(p.knowledgeAlternativeAllowed,true); assert.equal(p.lawEnforcementMayIncreaseDifficulty,false);
});

test('item restrito usa Vida nas ruas e preserva ajuste de lei como decisão do Mestre',()=>{
  const p=availabilityProfile({rarity:8,restricted:true,rarityModifier:2});
  assert.equal(p.effectiveRarity,10); assert.equal(p.skillKey,'callejeo'); assert.equal(p.difficulty,5); assert.equal(p.knowledgeAlternativeAllowed,false); assert.equal(p.lawEnforcementMayIncreaseDifficulty,true);
});

test('sem perfil local o motor não inventa modificador nem raridade efetiva',()=>{
  const p=availabilityProfile({rarity:6,restricted:false});
  assert.equal(p.rarityModifier,null); assert.equal(p.modifierKnown,false); assert.equal(p.effectiveRarity,null); assert.equal(p.difficulty,null);
  assert.equal(p.baseDifficulty,3);
});

test('referência de circunstâncias oficiais é consultável mas não aplicada automaticamente',()=>{
  assert.equal(rarityModifierReferenceById('primary_core_world').modifier,-2);
  assert.equal(rarityModifierReferenceById('outer_rim_world').modifier,2);
  assert.equal(rarityModifierReferenceById('wild_space_world').modifier,3);
  assert.equal(rarityModifierReferenceById('missing'),null);
});

test('pedido de aquisição nasce sem classificação inventada de planeta',()=>{
  const c=STARTER_PRESETS['jin-rio']();
  const pending=createPendingWeaponAcquisition('blaster_pistol',c,{requestId:'acq_r26',at:'2026-09-08T00:00:00.000Z'});
  assert.equal(pending.availability.modifierKnown,false); assert.equal(pending.availability.skillKey,'negociacion'); assert.equal(pending.availability.baseDifficulty,2);
});

test('modificador do local só entra por ação explícita e não muda os demais dados do pedido',()=>{
  const c=STARTER_PRESETS['jin-rio']();
  const pending=createPendingWeaponAcquisition('blaster_pistol',c,{requestId:'acq_r26',at:'2026-09-08T00:00:00.000Z'});
  const updated=setPendingWeaponAcquisitionRarityModifier(pending,-2);
  assert.equal(updated.requestId,pending.requestId); assert.equal(updated.listedPrice,pending.listedPrice); assert.equal(updated.rarity,pending.rarity);
  assert.equal(updated.availability.effectiveRarity,2); assert.equal(updated.availability.difficulty,1);
});

test('controller expõe cálculo auditado e persiste apenas modificador explicitamente definido',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'r26')); game.loadStarterCharacter('jin-rio');
  const legal=game.weaponAcquisitionAvailability('blaster_pistol',{rarityModifier:-2}); assert.equal(legal.effectiveRarity,2); assert.equal(legal.skillKey,'negociacion');
  const restricted=game.weaponAcquisitionAvailability('thermal_detonator',{rarityModifier:2}); assert.equal(restricted.effectiveRarity,10); assert.equal(restricted.skillKey,'callejeo');
  game.proposeWeaponAcquisition('blaster_pistol'); assert.equal(game.state.master.pendingAcquisition.availability.modifierKnown,false);
  game.setPendingWeaponAcquisitionRarityModifier(1); assert.equal(game.state.master.pendingAcquisition.availability.effectiveRarity,5); assert.equal(game.state.master.pendingAcquisition.availability.difficulty,2);
});

test('RAG EotE fundamenta raridade/disponibilidade no Core real sem autoridade AoR',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'r26-rag'));
  const result=game.consultRules('raridade disponibilidade item restrito negociação vida nas ruas',{limit:8});
  assert.equal(result.retrieval?.grounded ?? result.grounded,true);
  assert.ok(result.results.some(row=>row.id==='SW-EOTE-RARITY-AVAILABILITY-001'));
  assert.equal(result.results.some(row=>String(row.id).startsWith('SW-AOR-')),false);
});
