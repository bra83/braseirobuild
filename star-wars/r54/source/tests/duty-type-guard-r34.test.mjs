import test from 'node:test';
import assert from 'node:assert/strict';
import { createAorCoreCharacter, DUTY_TYPES_PT_BR } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
const base={name:'Rin',speciesKey:'human',careerKey:'soldier',specializationKey:'commando',partySize:1,careerFreeSkills:['atletismo','pelea','estrategia_militar','medicina'],specializationFreeSkills:['armas_cuerpo_a_cuerpo','aguante'],speciesChoiceSkills:['carisma','negociacion'],characteristics:{fortaleza:2,agilidad:2,intelecto:2,astucia:2,voluntad:2,presencia:2},dutyReductions:{},motivation:'Dever'};
test('criação AoR aceita somente Duty do catálogo canônico',()=>{
 for(const dutyType of DUTY_TYPES_PT_BR) assert.equal(createAorCoreCharacter({...base,dutyType}).duties[0].type,dutyType);
 assert.throws(()=>createAorCoreCharacter({...base,dutyType:'Duty inventado'}),/Tipo de Duty inválido/i);
 assert.throws(()=>createAorCoreCharacter({...base,dutyType:''}),/Tipo de Duty inválido/i);
});
