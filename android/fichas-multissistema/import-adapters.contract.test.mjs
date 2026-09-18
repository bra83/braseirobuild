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


test('maps generic D&D Braseiro sheet',()=>{
 const r=A.map({format:'braseiro-character-sheet',format_version:2,rules_profile:'',character:{name:'Borin Sete-Pregos',species:'Anão',class_name:'Mago',background:'Acólito',level:1,str:8,dex:14,con:15,int_score:15,wis:13,cha:10,max_hp:8,ac:12,initiative:'+2',proficiency_bonus:'+2',saving_throws:'Força -1',skills:'Acrobacia +2 • Arcanismo +4 ★',attacks:'Adaga | +4 | 1d4+2',spellcasting:'Inteligência',spells:'Raio de Fogo'}});
 assert.equal(r.system,'dnd55');assert.equal(r.character.species,'Anão');assert.equal(r.character.arcana,'4');assert.match(r.character.attacks,/Adaga/);
});

test('maps O Um Anel 2e sheet',()=>{
 const r=A.map({format:'braseiro-character',version:2,systemProfileId:'the-one-ring-2e',character:{name:'Burin Três-Colheres',culture:'Anões do Povo de Durin',vocation:'Erudito',sheet:{identity:{name:'Burin Três-Colheres',culture:'Anões do Povo de Durin',vocation:'Erudito'},attributes:{strength:5,heart:4,wits:5,enduranceCurrent:27,hopeCurrent:12,valor:1,wisdom:1},skills:[{name:'Ofício',rank:2}],combat:{parry:15,armor:'Proteção 1d',proficiencies:[],weapons:[]},inventory:[],other:{virtues:'Mão Austera'}}}});
 assert.equal(r.system,'tor2e');assert.equal(r.character.calling,'Erudito');assert.equal(r.character.craft,'2');assert.equal(r.character.parry,'15');
});

test('maps Pathfinder 2e rank sheet',()=>{
 const r=A.map({format:'braseiro-character-sheet',format_version:2,rules_profile:'pathfinder_2e',system_id:'pathfinder_2e',character:{name:'name',ancestry:'Gnomo',class_name:'Monge',background:'Guarda',level:1,str:3,dex:4,con:2,int_score:0,wis:1,cha:-1,max_hp:16,ac:17,perception_rank:'trained',skill_ranks:{acrobatics:'trained'}}});
 assert.equal(r.system,'pf2e');assert.equal(r.character.ancestry,'Gnomo');assert.equal(r.character.acrobatics,'1');assert.match(r.character.notes,/treinado/);
});

test('maps Forbidden Lands bilingual skill keys and resources',()=>{
 const r=A.map({format:'braseiro-forbidden-lands-character',schemaVersion:1,character:{name:'Bofri Pedra-Fria',kin:'Anão',profession:'Druida',age:'Adult',attributes:{strength:{current:4},agility:{current:2},wits:{current:4},empathy:{current:4}},skills:{'Tradição':3,Lore:1,Healing:1},talents:{Resistente:1},weapons:[{name:'Faca',quantity:1}],gear:[{name:'Bastão',quantity:1}],coins:{copper:0,silver:5,gold:0}}});
 assert.equal(r.system,'forbidden_lands');assert.equal(r.character.age,'Adulto');assert.equal(r.character.lore,'3');assert.match(r.character.gear,/Faca/);
});

test('maps Traveller careers, relations and unrepresented skills',()=>{
 const r=A.map({format:'braseiro-traveller-character',version:1,payload:{name:'Eric Almeida',homeworld:'Arkadia',age:30,attributes:{STR:7,DEX:9,END:5,INT:3,EDU:5,SOC:6},skills:{'Mecânica':0,'Corpo a Corpo':1,Arte:1},careers:[{term:3,ageStart:26,ageEnd:30,career:'Personalidade',assignment:'Artista de Performance',rankTitle:'Iniciante'}],relations:[{name:'Valentina Silva',type:'Inimigo',note:'Origem: Errante.'}],benefits:['Aliado'],credits:2000,shipShares:0,gear:[{name:'Comunicador',quantity:1}]}}); 
 assert.equal(r.system,'traveller');assert.equal(r.character.career,'Personalidade');assert.match(r.character.contacts,/Valentina Silva/);assert.equal(r.character.other_skills,'Arte 1');
});
