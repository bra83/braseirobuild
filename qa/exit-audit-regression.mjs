import assert from 'node:assert/strict';
import { validateMinimalCoreCharacter } from '../app/src/main/assets/www/core/wwn-rules-core.js';
const attrs={Strength:10,Dexterity:10,Constitution:10,Intelligence:10,Wisdom:10,Charisma:10};
assert.equal(validateMinimalCoreCharacter({level:1,attributes:attrs,skills:{Notice:1}}).ok,true);
assert.equal(validateMinimalCoreCharacter({level:7,attributes:attrs,skills:{Notice:3,Heal:4}}).ok,true,'progressed characters must remain legal');
assert.equal(validateMinimalCoreCharacter({level:11,attributes:attrs,skills:{Notice:3}}).ok,false);
assert.equal(validateMinimalCoreCharacter({level:7,attributes:attrs,skills:{Notice:5}}).ok,false);
console.log('EXIT_AUDIT_REGRESSION_PASS');
