import 'fake-indexeddb/auto';
import assert from 'node:assert/strict';
import { CharacterJsonService } from '../src/services/CharacterJsonService.ts';
import { CharacterRulesValidator } from '../src/services/CharacterRulesValidator.ts';

let failures = 0;
const check = (condition, label, detail='') => {
  if (condition) console.log('PASS', label);
  else { failures += 1; console.error('FAIL', label, detail); }
};

// Legal max-6 attributes must survive portable JSON normalization/import.
const payload = {
  format: 'braseiro.forbiddenlands.character',
  schemaVersion: 1,
  character: {
    id:'max6', name:'Max Seis', kin:'Humano', profession:'Guerreiro', age:'Adult',
    pride:'P', darkSecret:'S', relationships:[], appearance:'', reputation:0,
    attributes:{
      strength:{current:6,max:6}, agility:{current:3,max:3}, wits:{current:3,max:3}, empathy:{current:2,max:2}
    },
    skills:{}, talents:{}, willpowerPoints:0, conditions:[], criticalInjuries:[],
    weapons:[], armor:[], gear:[], encumbrance:{current:0,max:12},
    coins:{copper:0,silver:0,gold:0},
    resourceDice:{food:'d8',water:'d8',arrows:null,torches:null}, xp:0, notes:''
  }
};
const file = new File([JSON.stringify(payload)], 'max6.json', {type:'application/json'});
const imported = await CharacterJsonService.importFile(file);
check(imported[0]?.attributes?.strength?.max === 6, 'JSON preserva atributo máximo 6', JSON.stringify(imported[0]?.attributes?.strength));

// Existing sheet must reject structurally impossible attribute state.
const invalidExisting = structuredClone(payload.character);
invalidExisting.attributes.strength = {current:9,max:3};
const structural = CharacterRulesValidator.validateForSave(invalidExisting, false);
check(structural.valid === false, 'edição de ficha existente rejeita current > max', JSON.stringify(structural));

// Foundational identity should not silently accept unknown values on an existing sheet.
const invalidIdentity = structuredClone(payload.character);
invalidIdentity.kin = 'Ascendência inexistente';
const identity = CharacterRulesValidator.validateForSave(invalidIdentity, false);
check(identity.valid === false, 'edição de ficha existente rejeita ascendência inválida', JSON.stringify(identity));

if (failures) {
  console.error('CUSTOM_LONGTERM_AUDIT failures=', failures);
  process.exit(1);
}
console.log('CUSTOM_LONGTERM_AUDIT PASS');
