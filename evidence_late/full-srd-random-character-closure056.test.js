import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { enumerateLegalClassPaths } from '../app/src/main/assets/www/core/wwn-srd-class-combination.js';
import { classProfile } from '../app/src/main/assets/www/core/wwn-srd-class-catalog.js';
import { materializeRandomLevelOneCharacter } from '../app/src/main/assets/www/core/wwn-srd-random-character.js';
import { additionalLanguageSlots } from '../app/src/main/assets/www/core/wwn-character-creation-core.js';
import { CharacterRosterService } from '../app/src/main/assets/www/core/character-roster-service.js';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { RNG_ALGORITHM } from '../app/src/main/assets/www/core/rng-core.js';
import { projectCharacterForSheet, projectCharacterRosterForSheet } from '../app/src/main/assets/www/core/character-projection.js';

function pathKey(path){return path.kind==='FULL'?path.classId:`ADV:${path.partialClasses.join('+')}`;}
function expectedSpellCount(path){const ps=(path.kind==='FULL'?[path.classId]:path.partialClasses).map(classProfile);if(path.kind==='FULL')return ps[0].initialTraining.spellKnownFull??0;const c=ps.filter(p=>p.initialTraining.spellcasting===true);return c.length>=2?4:c.length===1?(c[0].initialTraining.spellKnownPartial??2):0;}
function makeCampaign(id='c56') {const p=new MemoryPersistenceAdapter();const state=makeEmptyCampaignState({campaignId:id,displayName:'Closure 056',masterSeed:'closure056-master'});p.createCampaign({campaignId:id,displayName:'Closure 056',stateJson:serializeCampaignState(state),masterSeed:'closure056-master',rngAlgorithm:RNG_ALGORITHM,initialSnapshotId:'s0'});return p;}

test('random level-one materialization is byte-deterministic for the same seed and id',()=>{const a=materializeRandomLevelOneCharacter({masterSeed:'same',characterId:'pc:same',name:'Mesmo',goal:'Explorar'});const b=materializeRandomLevelOneCharacter({masterSeed:'same',characterId:'pc:same',name:'Mesmo',goal:'Explorar'});assert.equal(a.status,'OK');assert.deepEqual(a,b);});

test('all 127 legal SRD class paths materialize into legal complete level-one characters',()=>{
  const paths=enumerateLegalClassPaths();assert.equal(paths.length,127);let full=0,adv=0;
  for(const [i,path] of paths.entries()){
    const r=materializeRandomLevelOneCharacter({masterSeed:`closure056-${i}`,characterId:`pc:${i}`,name:`Hero ${i}`,goal:'Explorar',classPath:path});assert.equal(r.status,'OK',pathKey(path));const c=r.character;
    if(path.kind==='FULL'){full++;assert.equal(c.classId,path.classId,pathKey(path));assert.deepEqual(c.partialClasses,[]);}else{adv++;assert.equal(c.classId,'ADVENTURER');assert.deepEqual(c.partialClasses,[...path.partialClasses]);}
    assert.equal(c.level,1);assert.ok(c.hp.max>=1);assert.equal(c.hp.current,c.hp.max);assert.equal(c.systemStrain.max,c.attributes.Constitution);assert.ok(Object.values(c.attributes).every(v=>Number.isInteger(v)&&v>=3&&v<=18));assert.ok(Object.values(c.skills).every(v=>v===0||v===1));
    assert.equal(c.languages.length,1+additionalLanguageSlots(c.skills).slots,pathKey(path));assert.equal(c.magic?.startingSpells?.length??0,expectedSpellCount(path),pathKey(path));
    assert.ok(Array.isArray(c.arts));assert.equal(c.creationProvenance.rule,'WWN_CHARACTER_CREATION_V2_SRD');assert.ok(c.creationProvenance.sourceRefs.length>3);
    const sheet=projectCharacterForSheet(c);assert.equal(sheet.status,'OK');assert.equal(sheet.characterId,c.characterId);assert.equal(sheet.spells.length,expectedSpellCount(path));assert.equal(sheet.arts.length,c.arts.length);
    for(const pool of sheet.effort){assert.ok(Number.isInteger(pool.max)&&pool.max>=1);assert.equal(pool.current,pool.max);}
  }
  assert.equal(full,6);assert.equal(adv,121);
});

test('source class exceptions survive materialization: Wise has no Effort, Invoker uses spell points, Vowed has Effort',()=>{
  const wise=materializeRandomLevelOneCharacter({masterSeed:'wise',characterId:'wise',name:'Wise',goal:'Saber',classPath:{kind:'ADVENTURER',partialClasses:['WISE','DUELIST']}});assert.equal(wise.status,'OK');assert.equal(wise.character.effort?.WISE,undefined);assert.ok(wise.character.arts.some(a=>a.classId==='WISE'));
  const inv=materializeRandomLevelOneCharacter({masterSeed:'inv',characterId:'inv',name:'Invoker',goal:'Invocar',classPath:{kind:'FULL',classId:'INVOKER'}});assert.equal(inv.status,'OK');assert.equal(inv.character.magic.castingMode,'SPELL_POINTS');assert.equal(inv.character.magic.startingSpells.length,4);assert.equal(inv.character.effort,null);
  const vowed=materializeRandomLevelOneCharacter({masterSeed:'vowed',characterId:'vowed',name:'Vowed',goal:'Disciplina',classPath:{kind:'ADVENTURER',partialClasses:['VOWED','HEALER']}});assert.equal(vowed.status,'OK');assert.ok(vowed.character.effort.VOWED.max>=1);assert.ok(vowed.character.arts.filter(a=>a.classId==='VOWED').length>=4);
});

test('roster generateRandom persists multiple heroes, joins party, and activation never rerolls them',()=>{
  const persistence=makeCampaign();const roster=new CharacterRosterService({persistence});
  const a=roster.generateRandom({campaignId:'c56',characterId:'pc:a',name:'A',goal:'A'});assert.equal(a.status,'OK');assert.equal(a.projectedState.characterState.characterId,'pc:a');assert.deepEqual(a.projectedState.partyState.groups[a.projectedState.partyState.activeGroupId].memberIds,['pc:a']);
  const countersAfterA=JSON.stringify(a.projectedState.rngState.counters);
  const b=roster.generateRandom({campaignId:'c56',characterId:'pc:b',name:'B',goal:'B'});assert.equal(b.status,'OK');assert.equal(b.projectedState.characterState.characterId,'pc:a');assert.ok(b.projectedState.partyState.groups[b.projectedState.partyState.activeGroupId].memberIds.includes('pc:b'));assert.equal(JSON.stringify(b.projectedState.rngState.counters),countersAfterA);
  const listed=roster.list({campaignId:'c56'});assert.equal(listed.status,'OK');assert.equal(listed.characters.length,2);assert.equal(listed.activeCharacterId,'pc:a');
  const activated=roster.activate({campaignId:'c56',characterId:'pc:b'});assert.equal(activated.status,'OK');assert.equal(activated.projectedState.characterState.characterId,'pc:b');assert.deepEqual(activated.projectedState.charactersState['pc:a'],a.character);assert.equal(JSON.stringify(activated.projectedState.rngState.counters),countersAfterA);
  const duplicate=roster.generateRandom({campaignId:'c56',characterId:'pc:b',name:'B2',goal:'B2'});assert.equal(duplicate.status,'CHARACTER_ID_EXISTS_NO_MUTATION');assert.equal(duplicate.revision,activated.projectedState.revision);
  const projected=projectCharacterRosterForSheet(activated.projectedState);assert.equal(projected.status,'OK');assert.equal(projected.characters.length,2);assert.equal(projected.activeCharacterId,'pc:b');
});

test('Closure 056 source has no Math.random/localStorage and keeps random character paths source-bound',()=>{
  for(const file of ['wwn-srd-random-character.js','wwn-srd-random-class.js','wwn-character-creation-core.js','character-creation-service.js','character-roster-service.js']){const src=fs.readFileSync(new URL(`../app/src/main/assets/www/core/${file}`,import.meta.url),'utf8');assert.equal(src.includes('Math.random'),false,file);assert.equal(src.includes('localStorage'),false,file);}
});
