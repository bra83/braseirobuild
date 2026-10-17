// Hardening V9 Braseiro TOR2E: source-only patch; never patch 4.9.211 APK in place.
// Usage: node ci/oua-4.9.212/harden-v9.mjs PATH_TO_EXTRACTED_V9
import fs from 'node:fs';
import path from 'node:path';

const file = path.resolve(process.argv[2] || '.', 'src/ui/bridges/Tor2eAdventureController.mjs');
if (!fs.existsSync(file)) throw new Error('V9 controller not found: ' + file);
let s = fs.readFileSync(file, 'utf8');
if (s.includes('HARDENED_V9_5_GATES')) {
  console.log('HARDENING_ALREADY_APPLIED');
  process.exit(0);
}
if (!s.includes('TOR 2E ADVENTURE STATE MACHINE CONTROLLER (V9)')) throw new Error('Controller V9 signature mismatch');
let count = 0;
function exact(oldText, newText, label) {
  const i = s.indexOf(oldText);
  if (i < 0 || s.indexOf(oldText, i + oldText.length) >= 0) throw new Error('Patch signature mismatch: ' + label);
  s = s.replace(oldText, newText);
  count++;
}
function regexp(re, newText, label) {
  const m = s.match(new RegExp(re.source, re.flags.includes('g') ? re.flags : re.flags + 'g'));
  if (!m || m.length !== 1) throw new Error('Patch regexp signature mismatch: ' + label);
  s = s.replace(re, newText);
  count++;
}
exact(
  '    this.authorizedCharacterSheet = options.authorizedCharacterSheet || null;',
  '    this.authorizedCharacterSheet = options.authorizedCharacterSheet || null;\n' +
  '    this.authorizeCombatAction = typeof options.authorizeCombatAction === "function" ? options.authorizeCombatAction : null;\n' +
  '    this.requireCommandId = options.requireCommandId === true;',
  'host-authorized constructor'
);
const derive = [
  '  #deriveAuthorizedSkillRank(skillName, rollParams, isProduction) {',
  '    if (isProduction) {',
  '      if (rollParams.authorizedCharacterSheet !== undefined || rollParams.forgedSkillRank === true) {',
  '        throw new Error("Ficha enviada pelo cliente não é autorizada em produção");',
  '      }',
  '      const sheet = this.authorizedCharacterSheet;',
  '      if (!sheet || !Array.isArray(sheet.skills)) throw new Error("Ficha autorizada pelo Host obrigatória");',
  '      const found = sheet.skills.find(s =>',
  '        String(s.name || s.skill).toLowerCase() === String(skillName).toLowerCase());',
  '      if (!found || !Number.isInteger(found.rank) || found.rank < 0 || found.rank > 6) {',
  '        throw new Error("Perícia ou graduação não autorizada: " + skillName);',
  '      }',
  '      if (rollParams.skillRank !== undefined && rollParams.skillRank !== found.rank) {',
  '        throw new Error(`Violação de integridade (EST-12): skillRank informado (${rollParams.skillRank}) diverge da ficha autorizada (${found.rank}) para \'${skillName}\'.`);',
  '      }',
  '      return found.rank;',
  '    }',
  '    const sheet = rollParams.authorizedCharacterSheet || this.authorizedCharacterSheet',
  '      || rollParams.character?.sheet || rollParams.character;',
  '    const found = Array.isArray(sheet?.skills) ? sheet.skills.find(s =>',
  '      String(s.name || s.skill).toLowerCase() === String(skillName).toLowerCase()) : null;',
  '    return found?.rank ?? rollParams.skillRank ?? 2;',
  '  }',
  '',
  '  performSceneCheck'
].join('\n');
regexp(/  #deriveAuthorizedSkillRank\(skillName, rollParams, isProduction\) \{[\s\S]*?\n  \}\n\n  performSceneCheck/,derive,'trusted character sheet');
exact(
  'rollParams.allowRetry === true && (checkDef.allowRetry === true || rollParams.retryRuleSource)',
  'rollParams.allowRetry === true && checkDef.allowRetry === true',
  'client-generated retry rule blocked'
);
exact(
  '    const commandId = rollParams.commandId || null;\n    if (commandId && this.processedCommands.has(commandId)) {',
  '    const commandId = rollParams.commandId || null;\n' +
  '    if (this.requireCommandId && (!commandId || typeof commandId !== "string")) throw new Error("commandId obrigatório");\n' +
  '    if (commandId && this.processedCommands.has(commandId)) {',
  'check command ID required in strict mode'
);
exact(
  "    this.#commitWithAtomicRollback(preSnapshot, charRef, charBackup, 'PERFORM_SCENE_CHECK');\n\n    const response = {",
  '    const response = {',
  'move check commit below cached command'
);
exact(
  '    if (commandId) {\n      this.processedCommands.set(commandId, JSON.parse(JSON.stringify(response)));\n    }\n\n    return response;\n  }\n\n  /**',
  "    if (commandId) {\n      this.processedCommands.set(commandId, JSON.parse(JSON.stringify(response)));\n    }\n    this.#commitWithAtomicRollback(preSnapshot, charRef, charBackup, 'PERFORM_SCENE_CHECK');\n    return response;\n  }\n\n  /**",
  'atomic check command persistence'
);
exact(
  '      this.persistenceStore.save(this.exportSnapshot(), { operationType });',
  '      const saved = this.persistenceStore.save(this.exportSnapshot(), { operationType });\n' +
  '      if (saved && typeof saved.then === "function") throw new Error("Promise não é persistência atômica síncrona");',
  'reject asynchronous persistence in sync API'
);
exact(
  '    const commandId = attackParams.commandId || null;\n    if (commandId && this.processedCommands.has(commandId)) {',
  '    const commandId = attackParams.commandId || null;\n' +
  '    if (this.requireCommandId && (!commandId || typeof commandId !== "string")) throw new Error("commandId obrigatório");\n' +
  '    if (commandId && this.processedCommands.has(commandId)) {',
  'combat command ID required in strict mode'
);
exact(
  "    if (targetIndex === -1) {\n      this.sceneState.combatResolved = true;\n      return { status: 'ALL_ENEMIES_ALREADY_DEFEATED', combatResolved: true };\n    }",
  "    if (targetIndex === -1) {\n" +
  "      if (this.sceneState.activeEnemies.every(e => e.isDefeated)) {\n" +
  "        this.sceneState.combatResolved = true;\n" +
  "        return { status: 'ALL_ENEMIES_ALREADY_DEFEATED', combatResolved: true };\n" +
  "      }\n" +
  "      throw new Error('Alvo não pertence ao combate ativo: ' + attackParams.targetEnemyId);\n" +
  "    }",
  'invalid target must not end combat'
);
const combatGuard = [
  "    const isProductionAttack = this.environment === 'production';",
  '    if (isProductionAttack) {',
  "      for (const field of ['mockFeatRolls','mockSuccessRolls','mockProtectionFeatRolls','mockProtectionSuccessRolls']) {",
  '        if (attackParams[field] !== undefined) throw new Error("Dado simulado proibido em produção: " + field);',
  '      }',
  "      for (const field of ['attacker','weapon','skillRank']) {",
  '        if (attackParams[field] !== undefined) throw new Error("Parâmetro de combate não autorizado: " + field);',
  '      }',
  '      if (typeof this.authorizeCombatAction !== "function") throw new Error("Autoridade do Host obrigatória no combate");',
  '    }',
  '    const approved = isProductionAttack ? this.authorizeCombatAction({',
  '      adventureId: this.activeAdventure.id, sceneId: this.currentSceneId,',
  '      targetEnemyId: this.sceneState.activeEnemies[targetIndex].id,',
  '      weaponId: attackParams.weaponId || null, attackerStance: attackParams.attackerStance || "OPEN"',
  '    }) : null;',
  '    if (isProductionAttack && (!approved || !approved.attacker || !approved.weapon',
  '      || !Number.isInteger(approved.skillRank) || approved.skillRank < 0 || approved.skillRank > 6',
  '      || !Number.isFinite(approved.weapon.damage) || approved.weapon.damage < 0 || approved.weapon.damage > 100)) {',
  '      throw new Error("Host recusou ficha, arma ou graduação");',
  '    }',
  '    const selectedAttack = isProductionAttack ? approved : attackParams;',
  '',
  '    const preSnapshot = this.exportSnapshot();',
  '    const defender = this.sceneState.activeEnemies[targetIndex];',
  '    const combatResult = this.combat.resolveAttack({'
].join('\n');
exact(
  '    const preSnapshot = this.exportSnapshot();\n    const defender = this.sceneState.activeEnemies[targetIndex];\n    const combatResult = this.combat.resolveAttack({',
  combatGuard,
  'host combat authorization'
);
exact(
  '      attacker: attackParams.attacker,\n      defender,\n      attackerStance: attackParams.attackerStance,\n      weapon: attackParams.weapon,\n      skillRank: attackParams.skillRank ?? 2,\n      spendHope: attackParams.spendHope ?? false,\n      mockFeatRolls: attackParams.mockFeatRolls,\n      mockSuccessRolls: attackParams.mockSuccessRolls,\n      mockProtectionFeatRolls: attackParams.mockProtectionFeatRolls,\n      mockProtectionSuccessRolls: attackParams.mockProtectionSuccessRolls',
  '      attacker: selectedAttack.attacker,\n      defender,\n      attackerStance: selectedAttack.attackerStance || attackParams.attackerStance,\n      weapon: selectedAttack.weapon,\n      skillRank: selectedAttack.skillRank ?? 2,\n      spendHope: isProductionAttack ? Boolean(selectedAttack.spendHope) : Boolean(attackParams.spendHope),\n      mockFeatRolls: isProductionAttack ? undefined : attackParams.mockFeatRolls,\n      mockSuccessRolls: isProductionAttack ? undefined : attackParams.mockSuccessRolls,\n      mockProtectionFeatRolls: isProductionAttack ? undefined : attackParams.mockProtectionFeatRolls,\n      mockProtectionSuccessRolls: isProductionAttack ? undefined : attackParams.mockProtectionSuccessRolls',
  'remove caller-controlled combat power'
);
exact(
  "    this.#commitWithAtomicRollback(preSnapshot, null, null, 'COMBAT_ATTACK');\n\n    const response = {",
  '    const response = {',
  'move combat commit below cached command'
);
exact(
  '    if (commandId) {\n      this.processedCommands.set(commandId, JSON.parse(JSON.stringify(response)));\n    }\n\n    return response;\n  }\n\n  getCurrentSceneView',
  "    if (commandId) {\n      this.processedCommands.set(commandId, JSON.parse(JSON.stringify(response)));\n    }\n    this.#commitWithAtomicRollback(preSnapshot, null, null, 'COMBAT_ATTACK');\n    return response;\n  }\n\n  getCurrentSceneView",
  'atomic combat command persistence'
);
s = s.replace('TOR 2E ADVENTURE STATE MACHINE CONTROLLER (V9)',
  'TOR 2E ADVENTURE STATE MACHINE CONTROLLER (V9 HARDENED_V9_5_GATES)');
fs.writeFileSync(file, s, 'utf8');
console.log('OUA_V9_HARDENING_APPLIED=' + count);
