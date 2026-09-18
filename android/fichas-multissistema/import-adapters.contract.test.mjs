import test from 'node:test';
import assert from 'node:assert/strict';
await import('./app/src/main/assets/www/import-adapters.js');
const A=globalThis.BRASEIRO_IMPORT_ADAPTERS;

test('maps native Tales Kid JSON into tftl sheet fields',()=>{
 const p={format:'braseiro-tales-kid',version:3,profile:'tales_kid',kid:{name:'Malin Nyström',archetype:'Popular',description:'Kid de teste',age:10,luck:5,xp:0,attributes:{CORPO:1,TECNOLOGIA:4,'CORAÇÃO':4,MENTE:1},skills:{Furtividade:0,'Força':1,'Movimentação':1,'Mecânica':0,'Programação':0,'Cálculo':1,Contatos:1,Charme:3,'Liderança':0,'Investigação':1,'Conhecimento':1,'Empatia':1},relationships:[{name:'Outro Kid',description:'amizade'}],inventory:[{name:'Chave inglesa antiga',bonus:2,iconic:true}],conditions:[],broken:false}};
 const r=A.map(p); assert.equal(r.system,'tftl'); assert.equal(r.character.name,'Malin Nyström'); assert.equal(r.character.archetype,'Popular Kid'); assert.equal(r.character.tech,'4'); assert.equal(r.character.charm,'3'); assert.match(r.character.relationships,/Outro Kid/); assert.match(r.character.gear,/Chave inglesa antiga/);
});

test('maps native WWN JSON without flattening class path, arts and magic',()=>{
 const p={format:'BRASEIRO_WWN_CHARACTER',system:'WWN',version:1,character:{name:'Aventureiro 2',classId:'ADVENTURER',partialClasses:['INVOKER','BLOOD_PRIEST'],level:1,xp:0,attributes:{Charisma:11,Constitution:6,Dexterity:14,Intelligence:9,Strength:13,Wisdom:8},baseAttackBonus:0,hp:{current:2,max:2},savingThrows:{Evasion:14,Luck:15,Mental:15,Physical:15},skills:{Magic:0,Notice:0,Pray:0,Shoot:0,Stab:0,Survive:0,Work:0},foci:[{focusId:'ALERT',name:'Alert'}],arts:[{name:'The Light of Faith'}],magic:{startingSpells:[{name:'Imperceptible Cerebral Divulgence'}]},effort:{BLOOD_PRIEST:{current:1,max:1}},languages:['Comum'],silver:80,systemStrain:{current:0,max:6},inventory:[]}};
 const r=A.map(p); assert.equal(r.system,'wwn'); assert.equal(r.character.class,'Aventureiro (classes parciais)'); assert.match(r.character.partial_classes,/INVOKER/); assert.equal(r.character.magic,'0'); assert.equal(r.character.hp,'2 / 2'); assert.match(r.character.arts_spells,/The Light of Faith/); assert.equal(r.character.system_strain,'0 / 6');
});
