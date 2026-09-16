import { buildManualCharacterDraft } from '../app/src/main/assets/www/core/wwn-manual-character-builder.js';
import { finalizeCharacterCreation } from '../app/src/main/assets/www/core/wwn-character-creation-core.js';

const attributes={Strength:14,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:9,Charisma:7};
const built=buildManualCharacterDraft({
  characterId:'manual-production-e2e',name:'Manual Production',goal:'Explorar o mundo',
  attributes,attributeGeneration:{mode:'STANDARD_ARRAY'},backgroundId:'ARTISAN',backgroundLearningPicks:['Connect','Convince'],
  path:{kind:'FULL',classId:'EXPERT'},classSelections:{EXPERT:{arts:[]}},startingSpells:[],
  focusSelections:[{focusId:'ALERT'},{focusId:'DIE_HARD'}],freeSkill:'Survive',
  linguaFranca:'Comum',nativeLanguage:'Comum',extraLanguages:['Antiga'],gear:{mode:'ROLL_SILVER'}
});
if(!built.ok) throw new Error(`manual builder failed: ${built.status}`);
const finalized=finalizeCharacterCreation({draft:built.draft,masterSeed:'manual-production-e2e'});
if(!finalized.ok) throw new Error(`manual finalize failed: ${finalized.status}`);
if(finalized.character.classId!=='EXPERT') throw new Error(`wrong production class dialect: ${finalized.character.classId}`);
if(finalized.character.level!==1||finalized.character.baseAttackBonus!==0) throw new Error('invalid expert level-one chassis');
if(!Number.isInteger(finalized.character.hp?.current)||finalized.character.hp.current<1) throw new Error('invalid starting hp');
console.log('MANUAL_CREATION_PRODUCTION PASS builder + finalize + current class dialect');
