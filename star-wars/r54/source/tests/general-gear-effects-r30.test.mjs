import test from 'node:test';
import assert from 'node:assert/strict';
import { opticalSetbackRelief, restraintEscapeProfile } from '../app/src/main/assets/vtt/js/rules/generalGearEffects.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('eletrobinóculos removem no máximo 1 Complicação causada por alcance ou iluminação',()=>{
  assert.deepEqual(opticalSetbackRelief('electrobinoculars',{setback:3,longRangeSetback:1,poorLightingSetback:1}),{
    itemId:'electrobinoculars',totalSetback:3,removedSetback:1,remainingSetback:2,
    source:{book:'La Era de la Rebelión - Libro de reglas',pdfPage:36,printedPage:35,section:'Electrobinoculares / Grilletes / Macrobinoculares',authority:'CROSS_LINE_REFERENCE',rulesProfile:'AOR_REFERENCE_ONLY',activeRulesAuthority:false}
  });
});

test('macrobinóculos só removem Complicação explicitamente atribuída ao longo alcance',()=>{
  assert.equal(opticalSetbackRelief('macrobinoculars',{setback:2,longRangeSetback:0,poorLightingSetback:1}).removedSetback,0);
  assert.equal(opticalSetbackRelief('macrobinoculars',{setback:2,longRangeSetback:1,poorLightingSetback:1}).removedSetback,1);
});

test('óptica nunca remove Complicação sem causa declarada nem aceita causas acima do total',()=>{
  assert.equal(opticalSetbackRelief('electrobinoculars',{setback:2}).remainingSetback,2);
  assert.throws(()=>opticalSetbackRelief('electrobinoculars',{setback:1,longRangeSetback:1,poorLightingSetback:1}),/não podem exceder/);
  assert.throws(()=>opticalSetbackRelief('comlink',{setback:1,longRangeSetback:1}),/sem efeito auditado/);
});

test('algemas usam teste Difícil de Atletismo ou Coordenação para escapar',()=>{
  const p=restraintEscapeProfile('binders');
  assert.equal(p.difficulty,3); assert.deepEqual(p.skills,['atletismo','coordinacion']); assert.equal(p.target,'living');
});

test('dispositivo de retenção de droide usa Disciplina Difícil e nova tentativa depende do Mestre',()=>{
  const p=restraintEscapeProfile('restraining_device');
  assert.equal(p.difficulty,3); assert.deepEqual(p.skills,['disciplina']); assert.equal(p.target,'droid'); assert.equal(p.retry,'gm_consent_after_failure');
});

test('GameController aplica alívio óptico só aos setbacks ambientais declarados',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'r30-optics'));
  game.loadStarterCharacter('zal');
  game.state.player.character.gear.push({id:'electrobinoculars',name:'Eletrobinóculos',quantity:1}); game.repository.save(game.state);
  const result=game.resolveSkillCheck({skillKey:'percepcion',difficulty:2,setback:3,gearAssist:{itemId:'electrobinoculars',longRangeSetback:1,poorLightingSetback:1},rng:()=>0});
  assert.equal(result.gearModifier.removedSetback,1);
  assert.equal(result.pool.setback,2);
});
