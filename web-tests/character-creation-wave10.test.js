import test from 'node:test';
import assert from 'node:assert/strict';
import { finalizeCharacterCreation, applyCreationSkillPicks, additionalLanguageSlots } from '../app/src/main/assets/www/core/wwn-character-creation-core.js';
import { CharacterCreationService } from '../app/src/main/assets/www/core/character-creation-service.js';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';

const SRC={sourceId:'SRC-WWN-SRD-1',locator:'test source-bound fixture'};
function expertDraft(){return {
  characterId:'pc:test',name:'Teste',goal:'Explorar o mundo',attributes:{Strength:10,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:14,Charisma:9},
  background:{backgroundId:'ARTISAN',freeSkill:'Craft',mode:'PICK',learningPicks:['Notice','Survive'],sourceRef:SRC},
  classContract:{classId:'FULL_EXPERT'},
  foci:[{focusId:'ALERT',category:'ANY',bonusSkills:['Notice'],sourceRef:SRC},{focusId:'SPECIALIST_SURVIVE',category:'NON_COMBAT',bonusSkills:['Survive'],sourceRef:SRC}],
  freeSkill:'Connect',linguaFranca:'Comum',nativeLanguage:'Comum',languages:['Comum','Antigo'],gear:{mode:'ROLL_SILVER'},
};}

test('creation skill picks cap at level 1 and demand replacement on third pick',()=>{const r=applyCreationSkillPicks({},['Notice','Notice','Notice']);assert.equal(r.ok,false);assert.equal(r.status,'SKILL_ALREADY_LEVEL_1_REPLACEMENT_REQUIRED');assert.equal(r.skills.Notice,1);});
test('language slots derive only from Know/Connect 0/1',()=>{assert.equal(additionalLanguageSlots({Connect:0}).slots,1);assert.equal(additionalLanguageSlots({Connect:1,Know:1}).slots,4);assert.equal(additionalLanguageSlots({}).slots,0);});
test('Full Expert creation finalizes source-bound level-1 character with deterministic HP/gear',()=>{const a=finalizeCharacterCreation({draft:expertDraft(),masterSeed:'creator-seed'});const b=finalizeCharacterCreation({draft:expertDraft(),masterSeed:'creator-seed'});assert.equal(a.ok,true);assert.deepEqual(a,b);assert.equal(a.character.level,1);assert.equal(a.character.classId,'FULL_EXPERT');assert.equal(a.character.skills.Notice,1);assert.equal(a.character.skills.Survive,1);assert.equal(a.character.inventory.length,0);assert.ok(a.character.silver>=30&&a.character.silver<=180);assert.equal(a.character.magic,null);});
test('creator rejects illegal focus category allocation without mutation',()=>{const d=expertDraft();d.foci=[{focusId:'A',category:'ANY',sourceRef:SRC},{focusId:'B',category:'COMBAT',sourceRef:SRC}];const r=finalizeCharacterCreation({draft:d,masterSeed:'x'});assert.equal(r.ok,false);assert.equal(r.status,'NON_COMBAT_FOCUS_REQUIREMENT_NOT_MET');});
test('Full Warrior requires combat focus plus any focus',()=>{const d=expertDraft();d.classContract={classId:'FULL_WARRIOR'};d.foci=[{focusId:'ARMSMASTER',category:'COMBAT',sourceRef:SRC},{focusId:'ALERT',category:'ANY',sourceRef:SRC}];const r=finalizeCharacterCreation({draft:d,masterSeed:'warrior'});assert.equal(r.ok,true);assert.equal(r.character.baseAttackBonus,1);assert.equal(r.character.classId,'FULL_WARRIOR');});
test('Full Mage finalization requires source-bound tradition level-one table and four source-bound spells',()=>{const d=expertDraft();d.classContract={classId:'FULL_MAGE',mageTradition:{traditionId:'HIGH_MAGE',spellcasting:true,sourceRef:SRC,fullClassTable:{1:{hitDice:'1d6-1',baseAttackBonus:0,sourceRef:SRC}}}};d.foci=[{focusId:'ARMORED_MAGIC',category:'ANY',sourceRef:SRC}];d.startingSpells=[1,2,3,4].map(i=>({spellId:`S${i}`,sourceRef:SRC}));const r=finalizeCharacterCreation({draft:d,masterSeed:'mage'});assert.equal(r.ok,true);assert.equal(r.character.magic.startingSpells.length,4);});
test('CharacterCreationService commits final character exactly once',()=>{const p=new MemoryPersistenceAdapter();const state=makeEmptyCampaignState({campaignId:'c',displayName:'C',masterSeed:'s'});p.createCampaign({campaignId:'c',displayName:'C',stateJson:serializeCampaignState(state),masterSeed:'s',rngAlgorithm:state.rngState.algorithm,initialSnapshotId:'s0'});const svc=new CharacterCreationService({persistence:p});const first=svc.finalize({campaignId:'c',draft:expertDraft()});assert.equal(first.status,'OK');const second=svc.finalize({campaignId:'c',draft:expertDraft()});assert.equal(second.status,'CHARACTER_ALREADY_FINALIZED_NO_MUTATION');assert.equal(p.loadCampaign('c').revision,1);});
