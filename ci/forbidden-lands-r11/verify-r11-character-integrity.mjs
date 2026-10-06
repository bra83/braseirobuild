import 'fake-indexeddb/auto';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { CharacterJsonService } from '../src/services/CharacterJsonService.ts';
import { CharacterRulesValidator } from '../src/services/CharacterRulesValidator.ts';

const base={
  id:'max6',name:'Max Seis',kin:'Humano',profession:'Guerreiro',age:'Adult',
  pride:'Orgulho',darkSecret:'Segredo',relationships:[],appearance:'',reputation:0,
  attributes:{strength:{current:6,max:6},agility:{current:3,max:3},wits:{current:3,max:3},empathy:{current:2,max:2}},
  skills:{Melee:5},talents:{'Pé Firme':3},willpowerPoints:10,conditions:[],criticalInjuries:[],
  weapons:[{id:'w',name:'Espada',quantity:1}],armor:[],gear:[],encumbrance:{current:1,max:12},
  coins:{copper:0,silver:0,gold:0},resourceDice:{food:'d8',water:'d8',arrows:null,torches:null},xp:12,notes:''
};

const file=new File([JSON.stringify({character:base})],'max6.json',{type:'application/json'});
const imported=await CharacterJsonService.importFile(file);
assert.equal(imported[0].attributes.strength.max,6,'JSON must preserve max 6');
assert.equal(imported[0].attributes.strength.current,6,'JSON must preserve current 6');
assert.equal(imported[0].skills.Melee,5,'JSON must preserve progressed skill 5');

const badAttr=structuredClone(base); badAttr.attributes.strength={current:9,max:3};
assert.equal(CharacterRulesValidator.validateForSave(badAttr,false).valid,false,'existing current>max must be rejected');
const badKin=structuredClone(base); badKin.kin='Ascendência inexistente';
assert.equal(CharacterRulesValidator.validateForSave(badKin,false).valid,false,'unknown existing kin must be rejected');
const badSkill=structuredClone(base); badSkill.skills.Melee=99;
assert.equal(CharacterRulesValidator.validateForSave(badSkill,false).valid,false,'corrupt skill must be rejected');

const editor=fs.readFileSync(new URL('../src/components/CharacterEditorV2.tsx',import.meta.url),'utf8');
assert.match(editor,/max="30" value=\{character\.attributes\[key\]\.max\}/);
assert.match(editor,/max=\{initialCharacter\?5:3\}/);
assert.match(editor,/disabled=\{Boolean\(initialCharacter\)\} value=\{character\.kin\}/);
assert.match(editor,/disabled=\{Boolean\(initialCharacter\)\} value=\{character\.profession\}/);
assert.match(editor,/disabled=\{Boolean\(initialCharacter\)\} value=\{character\.age\}/);

console.log('PASS R11 character integrity: max-6 JSON, progressed skills, structural save guard and immutable creation identity.');

// R11 build retry 2
