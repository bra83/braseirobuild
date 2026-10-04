import assert from 'node:assert/strict';
import 'fake-indexeddb/auto';
import { deleteDB } from 'idb';
import { CharacterGenerationService } from '../src/services/CharacterGenerationService.ts';
import { CharacterRulesValidator } from '../src/services/CharacterRulesValidator.ts';
import { ForbiddenLandsCharacterRules } from '../src/services/ForbiddenLandsCharacterRules.ts';
import { withDbRetry } from '../src/db/db.ts';
import { findEquipmentCatalogEntry } from '../src/data/forbidden-lands-encumbrance-catalog.ts';

await deleteDB('braseiro-db');
const RUNS = 500;
const seenNames = new Set();
const kinCount = new Map();
const professionCount = new Map();
const ageCount = new Map();

for (let i = 0; i < RUNS; i++) {
  await withDbRetry(async db => { await db.clear('rule_documents'); await db.clear('rule_chunks'); });
  const draft = await CharacterGenerationService.generateRandomCharacter('stress-campaign');
  assert.equal(draft.validation.valid, true, `draft ${i} invalid: ${draft.validation.errors.join('; ')}`);
  assert.ok(draft.validation.sources.some(source => source.documentId === 'embedded:forbidden-lands-player-handbook'));
  assert.ok([draft.kin.source,draft.profession.source,draft.age.source,...Object.values(draft.attributes).map(v=>v.source),...draft.skills.map(v=>v.source),...draft.talents.map(v=>v.source),...draft.equipment.map(v=>v.source)].every(source => String(source?.documentId || '').startsWith('embedded:')));

  const combination = ForbiddenLandsCharacterRules.creationCombination(draft.kin.label, draft.profession.label, draft.age.label);
  assert.ok(combination);
  assert.equal(Object.values(draft.attributes).reduce((sum,a)=>sum+a.max,0), combination.attributePoints);
  for (const [key,a] of Object.entries(draft.attributes)) { assert.ok(a.max >= 2 && a.max <= combination.attributeCaps[key]); assert.equal(a.current,a.max); }

  assert.equal(draft.skills.reduce((sum,s)=>sum+s.value,0), combination.skillPoints);
  assert.equal(new Set(draft.skills.map(s=>ForbiddenLandsCharacterRules.skillKey(s.label))).size, draft.skills.length);
  for (const s of draft.skills) { assert.ok(ForbiddenLandsCharacterRules.isKnownSkill(s.label)); assert.ok(s.value>=1 && s.value<=ForbiddenLandsCharacterRules.skillCap(draft.profession.label,s.label)); }

  assert.equal(draft.talents.reduce((sum,t)=>sum+(t.rank||1),0), combination.talentPicks);
  assert.equal(new Set(draft.talents.map(t=>ForbiddenLandsCharacterRules.normalize(t.label))).size, draft.talents.length);
  for (const item of draft.equipment) { assert.ok(findEquipmentCatalogEntry(item.label)); assert.ok(Number.isInteger(item.quantity ?? 1) && (item.quantity ?? 1) >= 1); }

  for (const resource of [draft.resources.food?.value,draft.resources.water?.value,draft.resources.arrows?.value]) assert.ok(resource==null || ['d6','d8','d10','d12'].includes(String(resource)));
  assert.ok(Number.isInteger(draft.resources.money?.value) && draft.resources.money.value >= 0);

  const normalizedName = draft.name.toLocaleLowerCase('pt-BR');
  assert.ok(!seenNames.has(normalizedName), `duplicate name ${draft.name}`);
  seenNames.add(normalizedName);

  const character = await CharacterGenerationService.convertDraftToCharacter(draft);
  const saveValidation = CharacterRulesValidator.validateForSave(character, true);
  assert.equal(saveValidation.valid, true, saveValidation.errors.join('; '));
  assert.equal(Object.values(character.attributes).reduce((sum,v)=>sum+v.max,0), combination.attributePoints);
  assert.equal(Object.values(character.skills).reduce((sum,v)=>sum+v,0), combination.skillPoints);

  await withDbRetry(db=>db.put('characters',character));
  const persisted=await withDbRetry(db=>db.get('characters',character.id));
  assert.ok(persisted);
  assert.deepEqual(persisted.attributes,character.attributes);
  assert.deepEqual(persisted.skills,character.skills);
  assert.deepEqual(persisted.talents,character.talents);
  assert.deepEqual(persisted.resourceDice,character.resourceDice);

  kinCount.set(character.kin,(kinCount.get(character.kin)||0)+1);
  professionCount.set(character.profession,(professionCount.get(character.profession)||0)+1);
  ageCount.set(character.age,(ageCount.get(character.age)||0)+1);
}
const characters=await withDbRetry(db=>db.getAll('characters'));
assert.equal(characters.length,RUNS);
assert.equal(seenNames.size,RUNS);
assert.ok(kinCount.size>=6);
assert.ok(professionCount.size>=6);
assert.equal(ageCount.size,3);
console.log(JSON.stringify({result:'PASS',generated:RUNS,persisted:characters.length,uniqueNames:seenNames.size,ruleLibraryRequired:false,kinDistribution:Object.fromEntries([...kinCount].sort()),professionDistribution:Object.fromEntries([...professionCount].sort()),ageDistribution:Object.fromEntries([...ageCount].sort())},null,2));