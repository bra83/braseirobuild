import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||'.');
const p=(...parts)=>path.join(root,...parts);

// 1) Portable JSON must preserve legal/runtime attribute maxima (including 6).
{
  const file=p('src','services','CharacterJsonService.ts');
  let s=fs.readFileSync(file,'utf8');
  if(!s.includes("import { CharacterRulesValidator } from './CharacterRulesValidator';")){
    s=s.replace("import { EncumbranceRulesEngine } from './EncumbranceRulesEngine';",
      "import { EncumbranceRulesEngine } from './EncumbranceRulesEngine';\nimport { CharacterRulesValidator } from './CharacterRulesValidator';");
  }
  s=s.replace("const max = integer(raw.max, fallback, 1, 5);","const max = integer(raw.max, fallback, 0, 30);");
  s=s.replace(
    "    const portable = normalizeCharacter(character);\n    const envelope: CharacterJsonEnvelope = {",
    "    const portable = normalizeCharacter(character);\n    const validation = CharacterRulesValidator.validateForSave(portable, false);\n    if (!validation.valid) throw new Error('A ficha de ' + portable.name + ' contém estado estrutural inválido: ' + validation.errors.join(' '));\n    const envelope: CharacterJsonEnvelope = {"
  );
  s=s.replace(
    "    return entries.map(normalizeCharacter);",
    "    return entries.map((entry, index) => {\n      const character = normalizeCharacter(entry);\n      const validation = CharacterRulesValidator.validateForSave(character, false);\n      if (!validation.valid) throw new Error(file.name + ': ficha ' + (index + 1) + ' inválida: ' + validation.errors.join(' '));\n      return character;\n    });"
  );
  fs.writeFileSync(file,s,'utf8');
}

// 2) Existing sheets get structural validation without re-applying starting budgets.
{
  const file=p('src','services','CharacterRulesValidator.ts');
  let s=fs.readFileSync(file,'utf8');
  const old="  /** Existing sheets keep legacy values; starting budgets apply only on first creation. */\n  static validateForSave(character: Character, isNew: boolean) {\n    if (isNew) return this.validateNewCharacter(character);\n    const errors = character?.name?.trim() ? [] : ['Informe o nome do personagem.'];\n    return { valid: errors.length === 0, errors };\n  }";
  const neu="  /** Starting budgets apply only on first creation; existing sheets still obey structural invariants. */\n  static validateForSave(character: Character, isNew: boolean) {\n    if (isNew) return this.validateNewCharacter(character);\n    return this.validateExistingCharacter(character);\n  }\n\n  static validateExistingCharacter(character: Character) {\n    const errors: string[] = [];\n    if (!character?.name?.trim()) errors.push('Informe o nome do personagem.');\n    if (!ForbiddenLandsCharacterRules.isKnownKin(character?.kin || '')) errors.push('Parentesco fora do catálogo conhecido.');\n    if (!ForbiddenLandsCharacterRules.isKnownProfession(character?.profession || '')) errors.push('Profissão fora do catálogo conhecido.');\n    if (!['Young','Adult','Old'].includes(character?.age || '')) errors.push('Idade inválida.');\n\n    const required: AttributeKey[] = ['strength','agility','wits','empathy'];\n    for (const key of required) {\n      const value = character?.attributes?.[key];\n      if (!value || !Number.isInteger(value.current) || !Number.isInteger(value.max) || value.max < 0 || value.max > 30 || value.current < 0 || value.current > value.max) {\n        errors.push('Atributo ' + key + ' corrompido: atual e máximo precisam ser inteiros, com 0 <= atual <= máximo <= 30.');\n      }\n    }\n\n    if (Object.entries(character?.skills || {}).some(([name, rank]) => !name.trim() || !Number.isInteger(rank) || rank < 0 || rank > 5)) errors.push('Há perícia com nome vazio ou categoria fora de 0–5.');\n    if (Object.entries(character?.talents || {}).some(([name, rank]) => !name.trim() || !Number.isInteger(rank) || rank < 0 || rank > 3)) errors.push('Há talento com nome vazio ou categoria fora de 0–3.');\n    if (!Number.isInteger(character?.willpowerPoints) || character.willpowerPoints < 0 || character.willpowerPoints > 10) errors.push('Pontos de Vontade devem estar entre 0 e 10.');\n    if (!Number.isInteger(character?.xp) || character.xp < 0) errors.push('Experiência deve ser um inteiro não negativo.');\n\n    const resourceDice = new Set(['d6','d8','d10','d12']);\n    for (const key of ['food','water','arrows','torches'] as const) {\n      const value = character?.resourceDice?.[key];\n      if (value != null && !resourceDice.has(String(value))) errors.push('Dado de recurso ' + key + ' inválido.');\n    }\n    const coins = character?.coins || { copper:0, silver:0, gold:0 };\n    if (Object.values(coins).some(value => !Number.isInteger(value) || value < 0)) errors.push('Moedas devem ser quantidades inteiras não negativas.');\n    const equipment = [...(character?.weapons || []), ...(character?.armor || []), ...(character?.gear || [])];\n    if (equipment.some(item => !item?.name?.trim() || !Number.isInteger(item.quantity ?? 1) || (item.quantity ?? 1) < 1)) errors.push('Há equipamento sem nome ou com quantidade inválida.');\n    return { valid: errors.length === 0, errors };\n  }";
  if(!s.includes(old)) throw new Error('R11 validator anchor missing');
  s=s.replace(old,neu);
  fs.writeFileSync(file,s,'utf8');
}

// 3) Editor must not invalidate max-6 or progressed skill 4/5 sheets.
// Foundational identity is immutable after creation in the normal editor.
{
  const file=p('src','components','CharacterEditorV2.tsx');
  let s=fs.readFileSync(file,'utf8');
  s=s.replaceAll('min="0" max="5" value={character.attributes[key].current}', 'min="0" max="30" value={character.attributes[key].current}');
  s=s.replaceAll('min="1" max="5" value={character.attributes[key].max}', 'min="0" max="30" value={character.attributes[key].max}');
  s=s.replace('PairEditor title="Perícias" value={character.skills} onChange={skills=>update({skills})} max={3}', 'PairEditor title="Perícias" value={character.skills} onChange={skills=>update({skills})} max={initialCharacter?5:3}');
  s=s.replace('<label>Parentesco<select value={character.kin} onChange={event=>update({kin:event.target.value})} className="field">', '<label>Parentesco<select disabled={Boolean(initialCharacter)} value={character.kin} onChange={event=>update({kin:event.target.value})} className="field disabled:opacity-60">');
  s=s.replace('<label>Profissão<select value={character.profession} onChange={event=>update({profession:event.target.value})} className="field">', '<label>Profissão<select disabled={Boolean(initialCharacter)} value={character.profession} onChange={event=>update({profession:event.target.value})} className="field disabled:opacity-60">');
  s=s.replace("<label>Idade<select value={character.age} onChange={event=>update({age:event.target.value as Character['age']})} className=\"field\">", "<label>Idade<select disabled={Boolean(initialCharacter)} value={character.age} onChange={event=>update({age:event.target.value as Character['age']})} className=\"field disabled:opacity-60\">");
  fs.writeFileSync(file,s,'utf8');
}

// 4) Version bump for update-in-place.
{
  const pkgPath=p('package.json');
  const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8')); pkg.version='3.3.7';
  fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n');
  const lockPath=p('package-lock.json');
  if(fs.existsSync(lockPath)){
    const lock=JSON.parse(fs.readFileSync(lockPath,'utf8'));
    lock.version='3.3.7';
    if(lock.packages?.['']) lock.packages[''].version='3.3.7';
    fs.writeFileSync(lockPath,JSON.stringify(lock,null,2)+'\n');
  }
  const scriptsDir=p('scripts');
  for(const name of fs.readdirSync(scriptsDir)){
    const file=path.join(scriptsDir,name);
    if(!fs.statSync(file).isFile()) continue;
    let s=fs.readFileSync(file,'utf8');
    s=s.replaceAll('3\\.3\\.6','3\\.3\\.7').replaceAll('3.3.6','3.3.7').replaceAll('303600','303700');
    fs.writeFileSync(file,s,'utf8');
  }
  const versionPath=p('src','version.ts');
  let version=fs.readFileSync(versionPath,'utf8');
  version=version.replaceAll('3.3.6','3.3.7').replaceAll('303600','303700').replace('Runtime 3.3.7 R10','Runtime 3.3.7 R11');
  fs.writeFileSync(versionPath,version,'utf8');

  const gradlePath=p('android','app','build.gradle');
  let gradle=fs.readFileSync(gradlePath,'utf8');
  gradle=gradle.replace(/versionCode\\s+\\d+/,'versionCode 303700');
  gradle=gradle.replace(/versionName\\s+"[^"]+"/,'versionName "3.3.7"');
  gradle=gradle.replaceAll('3.3.6','3.3.7').replaceAll('303600','303700');
  fs.writeFileSync(gradlePath,gradle,'utf8');
}
console.log('R11 character integrity patch applied.');
