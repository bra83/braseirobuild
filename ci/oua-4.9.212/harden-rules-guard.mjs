// V9 command bus hardening: block second attack path in Tor2eRulesGuard.
// Usage: node ci/oua-4.9.212/harden-rules-guard.mjs V9_EXTRACTED
import fs from 'node:fs';
import path from 'node:path';
const file=path.resolve(process.argv[2] || '.', 'src/application/Tor2eRulesGuard.mjs');
if(!fs.existsSync(file)) throw new Error('RulesGuard.mjs missing');
let s=fs.readFileSync(file,'utf8');
if(s.includes('HARDENED_RULES_GUARD_V9')) { console.log('GUARD_ALREADY_HARDENED'); process.exit(0); }
if(!s.includes('APPLICATION RULES GUARD & COMMAND BUS')) throw new Error('RulesGuard signature mismatch');
let n=0;
function change(a,b,label) {
 const i=s.indexOf(a);
 if(i<0||s.indexOf(a,i+a.length)>=0)throw new Error('Unsafe patch signature '+label);
 s=s.replace(a,b);n++;
}
change(
  "import { Tor2eRAGService } from '../library/Tor2eRAGService.mjs';",
  "import { Tor2eRAGService } from '../library/Tor2eRAGService.mjs';\nimport { Tor2eAdventureController } from '../ui/bridges/Tor2eAdventureController.mjs';",
  'import Tor2eAdventureController'
);
change(
  '  constructor() {\n    this.dice = new Tor2eDiceEngine();',
  '  constructor(options = {}) {\n    this.environment = options.environment || "test";\n' +
  '    this.authorizeCombatAction = typeof options.authorizeCombatAction === "function" ? options.authorizeCombatAction : null;\n' +
  '    this.authorizedCharacterSheet = options.authorizedCharacterSheet || null;\n' +
  '    this.getAuthorizedCharacterSheet = typeof options.getAuthorizedCharacterSheet === "function" ? options.getAuthorizedCharacterSheet : null;\n' +
  '    this.persistenceStore = options.persistenceStore || null;\n' +
  '    this.gmHelpAuthorizedByHost = options.gmHelpAuthorizedByHost === true;\n' +
  '    this.dice = new Tor2eDiceEngine();\n' +
  '    this.adventureController = new Tor2eAdventureController(options.adventures, {\n' +
  '      environment: this.environment,\n' +
  '      authorizedCharacterSheet: this.authorizedCharacterSheet,\n' +
  '      authorizeCombatAction: this.authorizeCombatAction,\n' +
  '      requireCommandId: this.environment === "production",\n' +
  '      hostGmSecretHash: options.hostGmSecretHash || null,\n' +
  '      hostSecretProvider: options.hostSecretProvider || null\n' +
  '    });',
  'constructor options'
);
change(
  '    const { commandId, kind, payload } = command;\n\n    // 1. Verificação de Idempotência\n    if (this.executedCommandIds.has(commandId)) {',
  '    const { commandId, kind, payload } = command;\n' +
  '    const production = this.environment === "production";\n' +
  '    if (production && (typeof commandId !== "string" || commandId.length === 0)) throw new Error("commandId obrigatório");\n' +
  '    const durableCommandIds = Array.isArray(campaignState?.ouaExecutedCommandIds) ? campaignState.ouaExecutedCommandIds : [];\n' +
  '    // HARDENED_RULES_GUARD_V9: replay must be rejected after save/reopen too.\n' +
  '    if (this.executedCommandIds.has(commandId) || (production && durableCommandIds.includes(commandId))) {',
  'durable idempotency'
);
change(
  "        userRole: payload.userRole || 'PLAYER'",
  "        userRole: production ? (this.gmHelpAuthorizedByHost ? 'GM' : 'PLAYER') : (payload.userRole || 'PLAYER')",
  'no caller GM role spoof'
);
change(
  '        const defenderId = payload.defender.id || payload.defender.name;',
  '        const defenderId = payload.defender?.id || payload.defender?.name;\n' +
  '        if (!defenderId) throw new Error("Alvo obrigatório");',
  'defender target'
);
change(
  '        if (!existingDefender) {',
  '        if (!existingDefender || existingDefender.isDefeated) {',
  'defeated targets rejected'
);
change(
  '        const attackResult = this.combat.resolveAttack({',
  [
    '        if (production) {',
    "          for (const field of ['mockFeatRolls','mockSuccessRolls','mockProtectionFeatRolls','mockProtectionSuccessRolls']) {",
    '            if (payload[field] !== undefined) throw new Error("Dados simulados proibidos: " + field);',
    '          }',
    "          for (const field of ['attacker','weapon','skillRank']) {",
    '            if (payload[field] !== undefined) throw new Error("Poderes de combate forjados pelo cliente: " + field);',
    '          }',
    '          if (typeof this.authorizeCombatAction !== "function") throw new Error("Host autorizador obrigatório");',
    '        }',
    '        const approved = production ? this.authorizeCombatAction({',
    '          targetEnemyId: defenderId, weaponId: payload.weaponId || null,',
    '          attackerStance: payload.attackerStance || "OPEN"',
    '        }) : payload;',
    '        if (production && (!approved || !approved.attacker || !approved.weapon',
    '          || !Number.isInteger(approved.skillRank) || approved.skillRank < 0 || approved.skillRank > 6',
    '          || !Number.isFinite(approved.weapon.damage) || approved.weapon.damage < 0 || approved.weapon.damage > 100)) {',
    '          throw new Error("Ataque não foi autorizado pelo Host");',
    '        }',
    '        const attackResult = this.combat.resolveAttack({'
  ].join('\n'),
  'combat authorized'
);
change(
  '          attacker: payload.attacker,\n          defender: existingDefender,\n          attackerStance: payload.attackerStance,\n          weapon: payload.weapon,\n          skillRank: payload.skillRank,\n          spendHope: payload.spendHope,\n          mockFeatRolls: payload.mockFeatRolls,\n          mockSuccessRolls: payload.mockSuccessRolls,\n          mockProtectionFeatRolls: payload.mockProtectionFeatRolls,\n          mockProtectionSuccessRolls: payload.mockProtectionSuccessRolls',
  '          attacker: approved.attacker,\n          defender: existingDefender,\n          attackerStance: approved.attackerStance || payload.attackerStance,\n          weapon: approved.weapon,\n          skillRank: approved.skillRank,\n          spendHope: production ? Boolean(approved.spendHope) : payload.spendHope,\n          mockFeatRolls: production ? undefined : payload.mockFeatRolls,\n          mockSuccessRolls: production ? undefined : payload.mockSuccessRolls,\n          mockProtectionFeatRolls: production ? undefined : payload.mockProtectionFeatRolls,\n          mockProtectionSuccessRolls: production ? undefined : payload.mockProtectionSuccessRolls',
  'combat values'
);
change(
  "      } else {\n        events.push({\n          type: 'GENERIC_ACTION_LOGGED',\n          actionText: payload.text\n        });\n      }",
  [
    "      } else if (['START_ADVENTURE','PERFORM_SCENE_CHECK','APPLY_FAIL_FORWARD','SCENE_COMBAT_ATTACK','TRANSITION_SCENE','CONCLUDE_ADVENTURE'].includes(actionType)) {",
    "        const preAdvSnap = this.adventureController.exportSnapshot();",
    "        if (updatedState.ouaAdventureSnapshot) {",
    "          this.adventureController.restoreFromSnapshot(updatedState.ouaAdventureSnapshot);",
    "        }",
    "        const hostSheet = typeof this.getAuthorizedCharacterSheet === 'function'",
    "          ? this.getAuthorizedCharacterSheet(updatedState, payload)",
    "          : (this.authorizedCharacterSheet || updatedState.authorizedCharacterSheet || updatedState.characterSheet || updatedState.character?.sheet || null);",
    "        this.adventureController.authorizedCharacterSheet = hostSheet;",
    "        try {",
    "          if (actionType === 'START_ADVENTURE') {",
    "            const view = this.adventureController.startAdventure(payload.adventureId);",
    "            updatedState = { ...updatedState, ouaAdventureSnapshot: this.adventureController.exportSnapshot(), ouaAdventureView: view };",
    "            events.push({ type: 'ADVENTURE_STARTED', adventureId: payload.adventureId, sceneId: view?.sceneId, view });",
    "          } else if (actionType === 'PERFORM_SCENE_CHECK') {",
    "            if (production && (payload.authorizedCharacterSheet !== undefined || payload.forgedSkillRank === true)) {",
    "              throw new Error('Ficha enviada pelo cliente não é autorizada em produção');",
    "            }",
    "            const charClone = updatedState.character ? JSON.parse(JSON.stringify(updatedState.character)) : null;",
    "            const checkRes = this.adventureController.performSceneCheck(payload.skill || payload.checkId, {",
    "              ...payload, commandId, character: charClone, authorizedCharacterSheet: undefined",
    "            });",
    "            if (charClone) updatedState = { ...updatedState, character: charClone };",
    "            const view = this.adventureController.getCurrentSceneView(production && this.gmHelpAuthorizedByHost ? 'GM' : 'PLAYER');",
    "            updatedState = { ...updatedState, ouaAdventureSnapshot: this.adventureController.exportSnapshot(), ouaAdventureView: view, lastAdventureCheckResult: checkRes };",
    "            events.push({ type: 'SCENE_CHECK_RESOLVED', ...checkRes });",
    "          } else if (actionType === 'APPLY_FAIL_FORWARD') {",
    "            const charClone = updatedState.character ? JSON.parse(JSON.stringify(updatedState.character)) : null;",
    "            const ffRes = this.adventureController.applyFailForwardPenalty(charClone, payload);",
    "            if (charClone) updatedState = { ...updatedState, character: charClone };",
    "            const view = this.adventureController.getCurrentSceneView(production && this.gmHelpAuthorizedByHost ? 'GM' : 'PLAYER');",
    "            updatedState = { ...updatedState, ouaAdventureSnapshot: this.adventureController.exportSnapshot(), ouaAdventureView: view, lastFailForwardResult: ffRes };",
    "            events.push({ type: 'SCENE_FAIL_FORWARD_APPLIED', ...ffRes });",
    "          } else if (actionType === 'SCENE_COMBAT_ATTACK') {",
    "            const atkRes = this.adventureController.performSceneCombatAttack({ ...payload, commandId });",
    "            const view = this.adventureController.getCurrentSceneView(production && this.gmHelpAuthorizedByHost ? 'GM' : 'PLAYER');",
    "            updatedState = { ...updatedState, ouaAdventureSnapshot: this.adventureController.exportSnapshot(), ouaAdventureView: view, lastSceneCombatResult: atkRes };",
    "            events.push({ type: 'SCENE_COMBAT_ATTACK_RESOLVED', ...atkRes });",
    "          } else if (actionType === 'TRANSITION_SCENE') {",
    "            const view = this.adventureController.transitionToScene(payload.nextSceneId);",
    "            updatedState = { ...updatedState, ouaAdventureSnapshot: this.adventureController.exportSnapshot(), ouaAdventureView: view };",
    "            events.push({ type: 'SCENE_TRANSITIONED', nextSceneId: payload.nextSceneId, view });",
    "          } else if (actionType === 'CONCLUDE_ADVENTURE') {",
    "            const charClone = updatedState.character ? JSON.parse(JSON.stringify(updatedState.character)) : {};",
    "            const concRes = this.adventureController.concludeAdventure(charClone);",
    "            if (concRes.status === 'ADVENTURE_CONCLUDED' && concRes.characterUpdates) {",
    "              updatedState = { ...updatedState, character: concRes.characterUpdates };",
    "            }",
    "            const view = this.adventureController.getCurrentSceneView(production && this.gmHelpAuthorizedByHost ? 'GM' : 'PLAYER');",
    "            updatedState = { ...updatedState, ouaAdventureSnapshot: this.adventureController.exportSnapshot(), ouaAdventureView: view, lastAdventureConclusion: concRes };",
    "            events.push({ type: 'ADVENTURE_CONCLUDE_RESULT', ...concRes });",
    "          }",
    "        } catch (err) {",
    "          this.adventureController.restoreFromSnapshot(preAdvSnap);",
    "          throw err;",
    "        }",
    "      } else {",
    "        events.push({",
    "          type: 'GENERIC_ACTION_LOGGED',",
    "          actionText: payload.text",
    "        });",
    "      }"
  ].join('\n'),
  'single ingress adventure controller actions'
);
change(
  "      this.executedCommandIds.add(commandId);\n\n      return {\n        commandId,\n        status: 'APPLIED',",
  [
    "      const preCommitAdvSnap = this.adventureController.exportSnapshot();",
    "      if (production) {",
    "        updatedState = { ...updatedState, ouaExecutedCommandIds: [...durableCommandIds, commandId] };",
    "      }",
    "      if (this.persistenceStore && typeof this.persistenceStore.save === 'function') {",
    "        try {",
    "          const saved = this.persistenceStore.save(updatedState, { commandId, actionType });",
    "          if (saved && typeof saved.then === 'function') throw new Error('Promise não é persistência atômica síncrona');",
    "        } catch (err) {",
    "          if (campaignState?.ouaAdventureSnapshot) {",
    "            this.adventureController.restoreFromSnapshot(campaignState.ouaAdventureSnapshot);",
    "          } else {",
    "            this.adventureController.restoreFromSnapshot(preCommitAdvSnap);",
    "          }",
    "          throw new Error(`Transação abortada com rollback atômico (${actionType}): ${err.message}`);",
    "        }",
    "      }",
    "      this.executedCommandIds.add(commandId);",
    "",
    "      return {",
    "        commandId,",
    "        status: 'APPLIED',"
  ].join('\n'),
  'record durable command replay key and atomic persistence commit'
);
fs.writeFileSync(file,s);
console.log('OUA_RULES_GUARD_HARDENED='+n);
