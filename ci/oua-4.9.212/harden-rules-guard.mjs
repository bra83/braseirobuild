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
  '  constructor() {\n    this.dice = new Tor2eDiceEngine();',
  '  constructor(options = {}) {\n    this.environment = options.environment || "test";\n' +
  '    this.authorizeCombatAction = typeof options.authorizeCombatAction === "function" ? options.authorizeCombatAction : null;\n' +
  '    this.gmHelpAuthorizedByHost = options.gmHelpAuthorizedByHost === true;\n' +
  '    this.dice = new Tor2eDiceEngine();',
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
  "      this.executedCommandIds.add(commandId);\n\n      return {\n        commandId,\n        status: 'APPLIED',",
  "      this.executedCommandIds.add(commandId);\n      if (production) {\n        updatedState = { ...updatedState, ouaExecutedCommandIds: [...durableCommandIds, commandId] };\n      }\n\n      return {\n        commandId,\n        status: 'APPLIED',",
  'record durable command replay key'
);
fs.writeFileSync(file,s);
console.log('OUA_RULES_GUARD_HARDENED='+n);
