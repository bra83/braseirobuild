import test from 'node:test';
import assert from 'node:assert/strict';
import { validateCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import {
  AOR_SPECIES,AOR_CAREERS,startingDutyForPartySize,applyDutyReductions,
  characteristicUpgradeCost,totalCharacteristicUpgradeCost,createAorCoreCharacter,creationPreview
} from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';

const humanBase={
  name:'Nira Valen',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
  careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],
  specializationFreeSkills:['astronavegacion','artilleria'],
  speciesChoiceSkills:['atletismo','medicina'],
  motivation:'Libertar mundos ocupados pelo Império.',background:'Pilota civil recrutada pela Aliança.',
  dutyType:'Superioridade espacial'
};

test('tabela oficial de Duty e política solo ficam distinguidas',()=>{
  assert.deepEqual(startingDutyForPartySize(2),{value:20,official:true});
  assert.deepEqual(startingDutyForPartySize(3),{value:15,official:true});
  assert.deepEqual(startingDutyForPartySize(4),{value:10,official:true});
  assert.deepEqual(startingDutyForPartySize(5),{value:10,official:true});
  assert.deepEqual(startingDutyForPartySize(6),{value:5,official:true});
  const solo=startingDutyForPartySize(1);
  assert.equal(solo.value,20); assert.equal(solo.official,false); assert.match(solo.note,/política local/i);
});

test('reduções de Duty cobrem as quatro opções e não permitem gasto maior que o Duty inicial',()=>{
  assert.deepEqual(applyDutyReductions(20,{xp10:true,credits2500:true}),{duty:0,xpBonus:10,creditBonus:2500,dutySpent:20});
  assert.deepEqual(applyDutyReductions(10,{xp5:true,credits1000:true}),{duty:0,xpBonus:5,creditBonus:1000,dutySpent:10});
  assert.throws(()=>applyDutyReductions(10,{xp10:true,credits1000:true}),/mais Duty/i);
});

test('custo de Característica é sequencial e respeita o máximo 5',()=>{
  assert.equal(characteristicUpgradeCost(2,4),70);
  assert.equal(characteristicUpgradeCost(1,5),140);
  assert.equal(totalCharacteristicUpgradeCost('human',{agilidad:3,intelecto:3}),60);
  assert.throws(()=>characteristicUpgradeCost(2,6),/inválido/i);
});

test('Humano personalizado gera ficha válida com sobreposição carreira/especialização até grau 2',()=>{
  const c=createAorCoreCharacter({...humanBase,characteristics:{agilidad:3,intelecto:3}});
  assert.equal(validateCharacter(c),true);
  assert.equal(c.source.type,'custom_aor_core');
  assert.equal(c.species,'Humano'); assert.equal(c.profession,'Ás');
  assert.deepEqual(c.specializations,['Piloto']);
  assert.equal(c.characteristics.agilidad,3); assert.equal(c.characteristics.intelecto,3);
  assert.equal(c.skills.astronavegacion,2); assert.equal(c.skills.artilleria,2);
  assert.equal(c.skills.atletismo,1); assert.equal(c.skills.medicina,1);
  assert.equal(c.derived.soak,2); assert.equal(c.derived.wounds.threshold,12); assert.equal(c.derived.strain.threshold,12);
  assert.equal(c.xp.total,110); assert.equal(c.xp.available,50);
  assert.equal(c.credits,500); assert.equal(c.duties[0].value,20); assert.equal(c.duties[0].officialStartingValue,false);
  assert.equal(c.motivations[0].text,humanBase.motivation);
});

test('Humano não pode usar como bônus racial uma perícia profissional',()=>{
  assert.throws(()=>createAorCoreCharacter({...humanBase,speciesChoiceSkills:['atletismo','astronavegacion']}),/não profissionais/i);
});

test('Droide recebe treinamento ampliado, Resistente e Proteção derivada',()=>{
  const c=createAorCoreCharacter({
    name:'RX-41',speciesKey:'droid',careerKey:'engineer',specializationKey:'mechanic',partySize:1,
    careerFreeSkills:['atletismo','computadoras','educacion','mecanica','percepcion','pilotar_vehiculo_espacial'],
    specializationFreeSkills:['pelea','mecanica','actividad_criminal'],
    motivation:'Provar que seres mecânicos podem escolher seu próprio dever.',
    dutyType:'Aquisição tecnológica',characteristics:{intelecto:3}
  });
  assert.equal(validateCharacter(c),true);
  assert.equal(c.skills.mecanica,2);
  assert.equal(c.skills.pelea,1);
  assert.deepEqual(c.talents,['Resistente']);
  assert.equal(c.derived.soak,2);
  assert.equal(c.derived.wounds.threshold,11);
  assert.equal(c.derived.strain.threshold,11);
  assert.equal(c.xp.total,175);
  assert.equal(c.xp.available,125); // Intelecto 1→3 custa 20+30
});

test('bônus racial e escolhas gratuitas nunca ultrapassam grau 2 durante a criação',()=>{
  assert.throws(()=>createAorCoreCharacter({
    name:'Sull',speciesKey:'sullustan',careerKey:'ace',specializationKey:'pilot',
    careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],
    specializationFreeSkills:['astronavegacion','artilleria'],
    motivation:'Voar pela Aliança.',dutyType:'Superioridade espacial'
  }),/grau 2/i);
});

test('Motivação é obrigatória e PE insuficientes impedem criação',()=>{
  assert.throws(()=>createAorCoreCharacter({...humanBase,motivation:''}),/Motivação é obrigatória/i);
  assert.throws(()=>createAorCoreCharacter({...humanBase,characteristics:{agilidad:5,intelecto:5}}),/PE insuficientes/i);
});

test('prévia soma bônus de Duty, créditos e PE de Características sem alterar estado',()=>{
  const p=creationPreview({...humanBase,dutyReductions:{xp10:true,credits2500:true},characteristics:{agilidad:3}});
  assert.equal(p.xpBudget,120); assert.equal(p.characteristicCost,30); assert.equal(p.xpRemaining,90);
  assert.equal(p.credits,3000); assert.equal(p.duty,0); assert.equal(p.dutySpent,20); assert.equal(p.dutyOfficial,false);
});

test('dados do núcleo AoR mantêm oito espécies, seis carreiras e três especializações por carreira',()=>{
  assert.equal(Object.keys(AOR_SPECIES).length,8);
  assert.equal(Object.keys(AOR_CAREERS).length,6);
  for(const career of Object.values(AOR_CAREERS)){
    assert.equal(career.skills.length,8);
    assert.equal(Object.keys(career.specializations).length,3);
    for(const spec of Object.values(career.specializations)) assert.equal(spec.skills.length,4);
  }
});
