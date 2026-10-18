import { newId } from '../core/ids.mjs';
import { ensureLivingWorldDepth, rememberNpc } from './livingWorldDepth.mjs';

const clone = v => structuredClone(v);
const now = () => new Date().toISOString();
const clamp = (n, min, max) => Math.max(min, Math.min(max, Number(n) || 0));

export const MISSION_STATUS = Object.freeze({
  AVAILABLE: 'AVAILABLE',
  ACTIVE: 'ACTIVE',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
  EXPIRED: 'EXPIRED',
  REJECTED: 'REJECTED'
});

export function ensureMissionOpportunities(world) {
  const next = ensureLivingWorldDepth(world);
  next.missionOpportunities = Array.isArray(next.missionOpportunities) ? next.missionOpportunities : [];
  return next;
}

function hash(s) {
  let h = 2166136261;
  for (const c of String(s)) {
    h ^= c.charCodeAt(0);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

export function generateMissionOpportunities(world, { maxActive = 6, force = false } = {}) {
  const next = ensureMissionOpportunities(world);
  const currentTick = Number(next.clock?.tick) || 0;
  const currentDay = Math.floor(currentTick / 4) + 1;
  const opportunities = next.missionOpportunities;

  // Count existing active/available
  const openCount = opportunities.filter(o => o.status === MISSION_STATUS.AVAILABLE || o.status === MISSION_STATUS.ACTIVE).length;
  if (!force && openCount >= maxActive) {
    return { world: next, generated: 0 };
  }

  const npcs = Object.values(next.npcs || {}).filter(n => {
    const s = next.livingDepth?.npcs?.[n.id];
    return s && s.lifeState === 'alive' && n.status !== 'dead';
  });
  if (!npcs.length) return { world: next, generated: 0 };

  const locations = Object.values(next.locations || {});
  const factions = Object.values(next.factions || {});
  let generated = 0;

  // Source 1: Faction Plans
  for (const f of factions) {
    if (openCount + generated >= maxActive) break;
    const plans = (f.plans || []).filter(p => p.status === 'active');
    for (const plan of plans) {
      const missionId = `mis_plan_${plan.id}_${f.id}`;
      if (opportunities.some(o => o.missionId === missionId || o.planId === plan.id)) continue;

      const patron = npcs.find(n => n.factionId === f.id) || npcs[hash(plan.id) % npcs.length];
      const targetLoc = locations.find(l => l.id !== patron.locationId) || locations[0] || { id: 'loc.core', name: 'Setor Central' };
      const patronLoc = next.locations?.[patron.locationId] || targetLoc;

      const planTitle = String(plan.title || plan.goal || plan.objective || plan.summary || 'Operação Estratégica');
      const planGoal = String(plan.goal || plan.objective || plan.title || plan.summary || 'Avançar objetivos da facção');

      const mission = {
        missionId,
        planId: plan.id,
        title: `Operação de Apoio: ${planTitle.slice(0, 50)}`,
        patronNpcId: patron.id,
        patronName: patron.name,
        factionId: f.id,
        factionName: f.name,
        locationId: patronLoc.id,
        locationName: patronLoc.name,
        targetLocationId: targetLoc.id,
        targetLocationName: targetLoc.name,
        stakes: `Avanço prioritário do plano estratégico da facção "${f.name}".`,
        rewards: {
          credits: 500 + (hash(plan.id) % 10) * 100,
          xp: 10,
          duty: f.id.includes('rebel') ? 5 : 0,
          obligationReduction: f.id.includes('syndicate') || f.id.includes('hutt') ? 5 : 0
        },
        createdTick: currentTick,
        deadlineTick: currentTick + 16, // 4 campaign days
        status: MISSION_STATUS.AVAILABLE,
        summary: `${patron.name} solicita apoio discreto para executar: ${planGoal}. O sucesso fortalecerá a posição de ${f.name} na região.`,
        checks: [
          { skill: 'Manha', difficulty: 'Média', purpose: 'coletar informações e preparar a aproximação' },
          { skill: 'Computadores', difficulty: 'Média', purpose: 'acessar terminais ou transferir registros de carga' }
        ],
        consequences: {
          successFactionRep: 15,
          failureFactionRep: -10,
          patronRelationDelta: 15
        },
        createdAt: now(),
        updatedAt: now()
      };

      opportunities.push(mission);
      generated++;
      break;
    }
  }

  // Source 2: Market Pressures
  for (const loc of locations) {
    if (openCount + generated >= maxActive) break;
    const locState = next.livingDepth?.locations?.[loc.id];
    if (locState && locState.marketPressure > 60) {
      const missionId = `mis_mkt_${loc.id}_${currentDay}`;
      if (opportunities.some(o => o.missionId === missionId)) continue;

      const patron = npcs.find(n => n.locationId === loc.id) || npcs[0];
      const mission = {
        missionId,
        title: `Escassez em ${loc.name} — Carga Emergencial`,
        patronNpcId: patron.id,
        patronName: patron.name,
        factionId: patron.factionId || null,
        locationId: loc.id,
        locationName: loc.name,
        targetLocationId: loc.id,
        targetLocationName: loc.name,
        stakes: `A pressão de mercado atingiu índice ${Math.round(locState.marketPressure)}. Mercadorias vitais precisam ser contrabandeadas ou fornecidas.`,
        rewards: {
          credits: 800 + (hash(missionId) % 8) * 100,
          xp: 10,
          obligationReduction: 5
        },
        createdTick: currentTick,
        deadlineTick: currentTick + 12, // 3 campaign days
        status: MISSION_STATUS.AVAILABLE,
        summary: `A alta demanda e escassez de suprimentos em ${loc.name} geraram um prêmio excepcional para quem conseguir transportar componentes médicos e peças sem atrair a fiscalização.`,
        checks: [
          { skill: 'Pilotagem (Espacial)', difficulty: 'Média', purpose: 'furar bloqueio ou patrulhas alfandegárias' },
          { skill: 'Negociação', difficulty: 'Média', purpose: 'maximizar a venda dos suprimentos' }
        ],
        consequences: {
          marketRelief: true,
          patronRelationDelta: 12
        },
        createdAt: now(),
        updatedAt: now()
      };

      opportunities.push(mission);
      generated++;
    }
  }

  return { world: next, generated };
}

export function acceptMissionOpportunity(world, missionId) {
  const next = ensureMissionOpportunities(world);
  const mission = next.missionOpportunities.find(m => m.missionId === String(missionId));
  if (!mission) throw new Error(`Missão "${missionId}" não encontrada.`);
  if (mission.status !== MISSION_STATUS.AVAILABLE) {
    throw new Error(`Missão "${missionId}" não está disponível para aceitação (status: ${mission.status}).`);
  }

  mission.status = MISSION_STATUS.ACTIVE;
  mission.acceptedAtTick = Number(next.clock?.tick) || 0;
  mission.updatedAt = now();

  // Record commitment in patron NPC's causal memory without telepathy
  const patronId = mission.patronNpcId;
  const dueDay = Math.floor(mission.deadlineTick / 4) + 1;
  const updatedWorld = rememberNpc(next, patronId, {
    kind: 'mission-accepted',
    summary: `Acordou com o grupo a execução da missão "${mission.title}".`,
    importance: 70,
    actor: 'player-group',
    commitment: mission.title,
    commitmentKey: `mis:${mission.missionId}`,
    dueDay
  });

  return { world: updatedWorld, mission: clone(mission) };
}

export function resolveMissionOpportunity(world, missionId, { outcome = MISSION_STATUS.COMPLETED, details = {} } = {}) {
  let next = ensureMissionOpportunities(world);
  const mission = next.missionOpportunities.find(m => m.missionId === String(missionId));
  if (!mission) throw new Error(`Missão "${missionId}" não encontrada.`);
  if (mission.status !== MISSION_STATUS.ACTIVE) {
    throw new Error(`Missão "${missionId}" precisa estar ATIVA para ser resolvida (status atual: ${mission.status}).`);
  }

  const isSuccess = outcome === MISSION_STATUS.COMPLETED;
  mission.status = isSuccess ? MISSION_STATUS.COMPLETED : MISSION_STATUS.FAILED;
  mission.resolvedAtTick = Number(next.clock?.tick) || 0;
  mission.resolutionOutcome = outcome;
  mission.resolutionNotes = details.notes || (isSuccess ? 'Missão concluída com êxito.' : 'Missão falhou na execução.');
  mission.updatedAt = now();

  const tick = Number(next.clock?.tick) || 0;
  const patronId = mission.patronNpcId;

  if (isSuccess) {
    const relDelta = mission.consequences?.patronRelationDelta || 15;
    next = rememberNpc(next, patronId, {
      tick,
      kind: 'mission-success',
      summary: `O grupo concluiu com êxito a missão "${mission.title}" conforme acordado.`,
      importance: 80,
      actor: 'player-group',
      relationshipDelta: relDelta,
      resolved: true
    });

    if (mission.factionId && next.livingDepth?.factions?.[mission.factionId]) {
      const fState = next.livingDepth.factions[mission.factionId];
      const rep = mission.consequences?.successFactionRep || 15;
      fState.relations = fState.relations || {};
      fState.relations['player-group'] = clamp((Number(fState.relations['player-group']) || 0) + rep, -100, 100);
      fState.powerIndex = clamp((Number(fState.powerIndex) || 50) + 2, 0, 100);
    }
  } else {
    next = rememberNpc(next, patronId, {
      tick,
      kind: 'mission-failed',
      summary: `O grupo falhou em cumprir os prazos ou requisitos da missão "${mission.title}".`,
      importance: 75,
      actor: 'player-group',
      relationshipDelta: -15,
      resolved: true
    });

    if (mission.factionId && next.livingDepth?.factions?.[mission.factionId]) {
      const fState = next.livingDepth.factions[mission.factionId];
      const rep = mission.consequences?.failureFactionRep || -10;
      fState.relations = fState.relations || {};
      fState.relations['player-group'] = clamp((Number(fState.relations['player-group']) || 0) + rep, -100, 100);
    }
  }

  return { world: next, mission: clone(mission), success: isSuccess };
}

export function rejectMissionOpportunity(world, missionId, { reason = '' } = {}) {
  const next = ensureMissionOpportunities(world);
  const mission = next.missionOpportunities.find(m => m.missionId === String(missionId));
  if (!mission) throw new Error(`Missão "${missionId}" não encontrada.`);
  if (mission.status !== MISSION_STATUS.AVAILABLE) {
    throw new Error(`Missão "${missionId}" não pode ser recusada (status: ${mission.status}).`);
  }

  mission.status = MISSION_STATUS.REJECTED;
  mission.rejectedAtTick = Number(next.clock?.tick) || 0;
  mission.rejectionReason = reason || 'Oportunidade dispensada pelo grupo.';
  mission.updatedAt = now();

  return { world: next, mission: clone(mission) };
}

export function advanceMissionOpportunities(world) {
  let next = ensureMissionOpportunities(world);
  const currentTick = Number(next.clock?.tick) || 0;
  const expired = [];
  const failed = [];

  for (const m of next.missionOpportunities) {
    if (m.deadlineTick !== null && currentTick > m.deadlineTick) {
      if (m.status === MISSION_STATUS.AVAILABLE) {
        m.status = MISSION_STATUS.EXPIRED;
        m.expiredAtTick = currentTick;
        m.updatedAt = now();
        expired.push(m.missionId);
      } else if (m.status === MISSION_STATUS.ACTIVE) {
        m.status = MISSION_STATUS.FAILED;
        m.failedAtTick = currentTick;
        m.failureReason = 'Prazo expirado antes do cumprimento dos objetivos.';
        m.updatedAt = now();
        failed.push(m.missionId);

        // Apply failure consequences to patron memory
        next = rememberNpc(next, m.patronNpcId, {
          tick: currentTick,
          kind: 'mission-expired-active',
          summary: `O prazo da missão "${m.title}" venceu sem que o grupo entregasse os resultados prometidos.`,
          importance: 75,
          actor: 'player-group',
          relationshipDelta: -10,
          resolved: true
        });

        if (m.factionId && next.livingDepth?.factions?.[m.factionId]) {
          const fState = next.livingDepth.factions[m.factionId];
          fState.relations = fState.relations || {};
          fState.relations['player-group'] = clamp((Number(fState.relations['player-group']) || 0) - 5, -100, 100);
        }
      }
    }
  }

  return { world: next, expired, failed };
}

export function listMissionOpportunities(world, { status = null, patronNpcId = null, locationId = null, factionId = null } = {}) {
  const next = ensureMissionOpportunities(world);
  return next.missionOpportunities.filter(m => {
    if (status && m.status !== status) return false;
    if (patronNpcId && m.patronNpcId !== patronNpcId) return false;
    if (locationId && m.locationId !== locationId && m.targetLocationId !== locationId) return false;
    if (factionId && m.factionId !== factionId) return false;
    return true;
  }).map(clone);
}
