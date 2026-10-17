export const MAX_MANEUVERS_PER_TURN = 2;
export const EXTRA_MANEUVER_STRAIN_COST = 2;
import { normalizePlayerConditions } from './conditions.mjs';

export function createCombatState() {
  return {
    active: false,
    round: 0,
    socialEncounterSerial: 0,
    playerTurn: { actionUsed: false, maneuversUsed: 0, expertSkillTalentUsed:false },
    playerConditions: normalizePlayerConditions(null),
    lastAttack: null,
    medicineUsed: false,
    pendingAttackBoost: 0,
    pendingNpcBoost: 0,
    adversaries: [],
    wingGuardOrders: [],
    encounter: null,
    log: []
  };
}

function cloneCombat(combat) {
  return structuredClone(combat ?? createCombatState());
}

export function startCombat(combat = createCombatState()) {
  const next = cloneCombat(combat);
  next.active = true;
  next.socialEncounterSerial = (Number.isSafeInteger(next.socialEncounterSerial) && next.socialEncounterSerial >= 0 ? next.socialEncounterSerial : 0) + 1;
  next.round = Math.max(1, Number(next.round) || 0);
  next.playerTurn = { actionUsed: false, maneuversUsed: 0, expertSkillTalentUsed:false };
  next.playerConditions = normalizePlayerConditions(null);
  next.medicineUsed = false;
  next.pendingAttackBoost = 0;
  next.pendingNpcBoost = 0;
  next.adversaries = [];
  next.wingGuardOrders = [];
  delete next.arrow7TurretOrder;
  next.encounter = null;
  delete next.aorFieldCommandHistory;
  delete next.aorCommandTokens;
  delete next.aorCompanionCover;
  delete next.aorCoordinatedAssault;
  delete next.bodyGuard;
  delete next.npcCompanionTargetId;
  delete next.aorCriticalAid;
  delete next.pendingNpcBlasts;
  delete next.aorBlastEscapes;
  delete next.hardHeadedRecovery;
  delete next.causalHookId;
  delete next.causalHookSummary;
  next.log.push({ type: 'COMBATE_INICIADO', round: next.round });
  return next;
}

export function nextPlayerTurn(combat) {
  if (!combat?.active) throw new Error('O combate ainda não foi iniciado.');
  const next = cloneCombat(combat);
  next.round += 1;
  next.playerTurn = { actionUsed: false, maneuversUsed: 0, expertSkillTalentUsed:false };
  next.log.push({ type: 'NOVO_TURNO', round: next.round });
  return next;
}

export function endCombat(combat) {
  const next = cloneCombat(combat);
  next.active = false;
  next.playerTurn = { actionUsed: false, maneuversUsed: 0, expertSkillTalentUsed:false };
  next.playerConditions = normalizePlayerConditions(null);
  next.pendingAttackBoost = 0;
  next.pendingNpcBoost = 0;
  next.adversaries = [];
  next.wingGuardOrders = [];
  delete next.arrow7TurretOrder;
  next.encounter = null;
  delete next.aorFieldCommandHistory;
  delete next.aorCommandTokens;
  delete next.aorCompanionCover;
  delete next.aorCoordinatedAssault;
  delete next.bodyGuard;
  delete next.npcCompanionTargetId;
  delete next.aorCriticalAid;
  delete next.pendingNpcBlasts;
  delete next.aorBlastEscapes;
  delete next.hardHeadedRecovery;
  delete next.causalHookId;
  delete next.causalHookSummary;
  next.log.push({ type: 'COMBATE_ENCERRADO', round: next.round });
  return next;
}

export function spendAction(combat, label = 'Ação') {
  if (!combat?.active) return cloneCombat(combat);
  if (combat.playerTurn?.actionUsed) throw new Error('A ação deste turno já foi usada.');
  const next = cloneCombat(combat);
  next.playerTurn.actionUsed = true;
  delete next.aorBracePrepared;
  next.log.push({ type: 'ACAO', round: next.round, label });
  return next;
}

export function spendManeuver(combat, { method = 'normal', label = 'Manobra', forceFirstStrain = false } = {}) {
  if (!combat?.active) return { combat: cloneCombat(combat), strainCost: 0 };
  const used = Number(combat.playerTurn?.maneuversUsed ?? 0);
  if (used >= MAX_MANEUVERS_PER_TURN) throw new Error('Não é permitido realizar mais de 2 manobras no turno.');

  const next = cloneCombat(combat);
  let strainCost = 0;

  if (used === 0 && forceFirstStrain) {
    if (method !== 'strain') throw new Error('Sem manobra gratuita: esta manobra exige sofrer 2 de Tensão.');
    strainCost = EXTRA_MANEUVER_STRAIN_COST;
  } else if (used === 1) {
    if (method === 'strain') {
      strainCost = EXTRA_MANEUVER_STRAIN_COST;
    } else if (method === 'action') {
      if (next.playerTurn.actionUsed) throw new Error('A ação já foi usada e não pode ser trocada por uma segunda manobra.');
      next.playerTurn.actionUsed = true;
    } else {
      throw new Error('A segunda manobra exige sofrer 2 de Tensão ou trocar a ação do turno.');
    }
  }

  next.playerTurn.maneuversUsed = used + 1;
  next.log.push({ type: 'MANOBRA', round: next.round, label, method: used === 0 && !forceFirstStrain ? 'normal' : method });
  return { combat: next, strainCost };
}
