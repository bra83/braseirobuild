import assert from 'node:assert/strict';
import 'fake-indexeddb/auto';
import { deleteDB } from 'idb';

import { CharacterGenerationService } from '../../src/services/CharacterGenerationService.ts';
import { CharacterRulesValidator } from '../../src/services/CharacterRulesValidator.ts';
import { ForbiddenLandsCharacterRules } from '../../src/services/ForbiddenLandsCharacterRules.ts';
import { withDbRetry } from '../../src/db/db.ts';
import { findEquipmentCatalogEntry } from '../../src/data/forbidden-lands-encumbrance-catalog.ts';

await deleteDB('braseiro-db');

const RUNS = 500;
const seenNames = new Set();
const kinCount = new Map();
const professionCount = new Map();
const ageCount = new Map();

const failures = [];

for (let i = 0; i < RUNS; i++) {
  try {
    // Prove every iteration is independent of imported rule PDFs/RAG.
    await withDbRetry(async db => {
      await db.clear('rule_documents');
      await db.clear('rule_chunks');
    });

    const draft = await CharacterGenerationService.generateRandomCharacter('stress-campaign');

    assert.equal(draft.validation.valid, true, 'draft validation');
    assert.ok(draft.validation.sources.some(s => s.documentId === 'embedded:forbidden-lands-player-handbook'), 'embedded source');
    assert.ok([
      draft.kin.source,
      draft.profession.source,
      draft.age.source,
      ...Object.values(draft.attributes).map(v => v.source),
      ...draft.skills.map(v => v.source),
      ...draft.talents.map(v => v.source),
      ...draft.equipment.map(v => v.source),
    ].every(s => String(s?.documentId || '').startsWith('embedded:')), 'all mechanical sources embedded');

    const combination = ForbiddenLandsCharacterRules.creationCombination(draft.kin.label, draft.profession.label, draft.age.label);
    assert.ok(combination, 'valid creation combination');

    const attrTotal = Object.values(draft.attributes).reduce((sum, a) => sum + a.max, 0);
    assert.equal(attrTotal, combination.attributePoints, 'attribute budget');
    for (const [key, value] of Object.entries(draft.attributes)) {
      assert.ok(value.max >= 2 && value.max <= combination.attributeCaps[key], 'attribute cap');
      assert.equal(value.current, value.max, 'attribute current=max');
    }

    const skillTotal = draft.skills.reduce((sum, s) => sum + s.value, 0);
    assert.equal(skillTotal, combination.skillPoints, 'skill budget');
    assert.equal(new Set(draft.skills.map(s => ForbiddenLandsCharacterRules.skillKey(s.label))).size, draft.skills.length, 'no duplicate skills');
    for (const s of draft.skills) {
      assert.ok(ForbiddenLandsCharacterRules.isKnownSkill(s.label), 'known skill');
      assert.ok(s.value >= 1 && s.value <= ForbiddenLandsCharacterRules.skillCap(draft.profession.label, s.label), 'skill cap');
    }

    const talentPoints = draft.talents.reduce((sum, t) => sum + (t.rank || 1), 0);
    assert.equal(talentPoints, combination.talentPicks, 'talent budget');
    assert.equal(new Set(draft.talents.map(t => ForbiddenLandsCharacterRules.normalize(t.label))).size, draft.talents.length, 'no duplicate talents');

    assert.ok(draft.equipment.length > 0, 'starting equipment exists');
    for (const item of draft.equipment) {
      assert.ok(findEquipmentCatalogEntry(item.label), 'equipment is cataloged');
      assert.ok(Number.isInteger(item.quantity ?? 1) && (item.quantity ?? 1) >= 1, 'equipment quantity');
    }

    assert.ok(['d6','d8','d10','d12'].includes(String(draft.resources.food?.value)), 'food die');
    assert.ok(['d6','d8','d10','d12'].includes(String(draft.resources.water?.value)), 'water die');
    assert.ok(draft.resources.arrows?.value == null || ['d6','d8','d10','d12'].includes(String(draft.resources.arrows.value)), 'arrows die');
    assert.ok(Number.isInteger(draft.resources.money?.value) && draft.resources.money.value >= 0, 'money');

    const normalizedName = draft.name.toLocaleLowerCase('pt-BR');
    assert.ok(!seenNames.has(normalizedName), 'unique generated name');
    seenNames.add(normalizedName);

    const character = await CharacterGenerationService.convertDraftToCharacter(draft);
    const saveValidation = CharacterRulesValidator.validateForSave(character, true);
    assert.equal(saveValidation.valid, true, saveValidation.errors.join('; '));

    await withDbRetry(db => db.put('characters', character));
    const persisted = await withDbRetry(db => db.get('characters', character.id));
    assert.ok(persisted, 'persisted character');
    assert.deepEqual(persisted.attributes, character.attributes, 'attributes persist');
    assert.deepEqual(persisted.skills, character.skills, 'skills persist');
    assert.deepEqual(persisted.talents, character.talents, 'talents persist');
    assert.deepEqual(persisted.resourceDice, character.resourceDice, 'resources persist');

    kinCount.set(character.kin, (kinCount.get(character.kin) || 0) + 1);
    professionCount.set(character.profession, (professionCount.get(character.profession) || 0) + 1);
    ageCount.set(character.age, (ageCount.get(character.age) || 0) + 1);
  } catch (error) {
    failures.push({ index: i, message: String(error?.stack || error) });
    break;
  }
}

assert.equal(failures.length, 0, failures[0]?.message || 'unexpected stress failure');
assert.equal(seenNames.size, RUNS, 'all generated names must be unique');

const characters = await withDbRetry(db => db.getAll('characters'));
assert.equal(characters.length, RUNS, 'all characters persisted');

console.log(JSON.stringify({
  result: 'PASS',
  generated: RUNS,
  persisted: characters.length,
  uniqueNames: seenNames.size,
  kinDistribution: Object.fromEntries([...kinCount].sort()),
  professionDistribution: Object.fromEntries([...professionCount].sort()),
  ageDistribution: Object.fromEntries([...ageCount].sort()),
}, null, 2));
