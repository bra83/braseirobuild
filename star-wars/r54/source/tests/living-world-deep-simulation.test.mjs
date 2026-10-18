import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createWorld,
  createLocation,
  upsertLocation,
  createFaction,
  upsertFaction,
  createNpc,
  upsertNpc
} from '../app/src/main/assets/vtt/js/world/world.mjs';
import {
  connectLocations,
  normalizeSandboxWorld
} from '../app/src/main/assets/vtt/js/world/sandbox.mjs';
import {
  advanceLivingWorldDepth,
  livingWorldSnapshot,
  livingWorldSummary,
  rememberNpc
} from '../app/src/main/assets/vtt/js/world/livingWorldDepth.mjs';
import {
  ensureMissionOpportunities,
  generateMissionOpportunities,
  acceptMissionOpportunity,
  resolveMissionOpportunity,
  advanceMissionOpportunities,
  listMissionOpportunities,
  MISSION_STATUS
} from '../app/src/main/assets/vtt/js/world/missionOpportunityEngine.mjs';
import {
  createOriginalAdventure,
  validateOriginalAdventure
} from '../app/src/main/assets/vtt/js/master/originalAdventureCreator.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';

function roundTrip(val) {
  return JSON.parse(JSON.stringify(val));
}

test('Living World Deep Simulation: 100 NPCs persistentes, 30 dias de simulação e sem telepatia', () => {
  let w = createWorld();

  // Create 6 rich locations
  const locations = [
    createLocation({ id: 'loc.tatooine.cantina', name: 'Cantina de Mos Eisley', knownToPlayer: true, tags: ['tatooine', 'social', 'crime'] }),
    createLocation({ id: 'loc.tatooine.docks', name: 'Hangar das Docas de Mos Eisley', knownToPlayer: true, tags: ['tatooine', 'porto', 'oficina'] }),
    createLocation({ id: 'loc.coruscant.underworld', name: 'Subnível 1313 de Coruscant', knownToPlayer: true, tags: ['coruscant', 'underworld'] }),
    createLocation({ id: 'loc.coruscant.senate', name: 'Distrito Governamental de Coruscant', knownToPlayer: true, tags: ['coruscant', 'politica'] }),
    createLocation({ id: 'loc.moncala.depths', name: 'Minas Oceânicas de Mon Cala', knownToPlayer: true, tags: ['mon-cala', 'mineracao'] }),
    createLocation({ id: 'loc.corellia.shipyards', name: 'Estaleiros Centrais de Corellia', knownToPlayer: true, tags: ['corellia', 'estaleiro', 'industria'] })
  ];
  for (const l of locations) w = upsertLocation(w, l);

  // Connect routes
  w = connectLocations(w, { fromLocationId: 'loc.tatooine.cantina', toLocationId: 'loc.tatooine.docks', ticks: 1 });
  w = connectLocations(w, { fromLocationId: 'loc.tatooine.docks', toLocationId: 'loc.corellia.shipyards', ticks: 8 });
  w = connectLocations(w, { fromLocationId: 'loc.corellia.shipyards', toLocationId: 'loc.coruscant.senate', ticks: 6 });
  w = connectLocations(w, { fromLocationId: 'loc.coruscant.senate', toLocationId: 'loc.coruscant.underworld', ticks: 2 });
  w = connectLocations(w, { fromLocationId: 'loc.coruscant.underworld', toLocationId: 'loc.moncala.depths', ticks: 12 });

  // Create 4 active factions with plans
  const factions = [
    createFaction({ id: 'fac.alliance', name: 'Aliança para Restauração da República', knownToPlayer: true, objective: 'Restaurar a liberdade' }),
    createFaction({ id: 'fac.empire', name: 'Império Galáctico — ISB e Frota', knownToPlayer: false, objective: 'Ordem através da submissão' }),
    createFaction({ id: 'fac.hutt', name: 'Cartel Hutt', knownToPlayer: true, objective: 'Monopólio do contrabando' }),
    createFaction({ id: 'fac.miners', name: 'Sindicato Quarren', knownToPlayer: true, objective: 'Autonomia comercial' })
  ];
  factions[0].plans = [{ id: 'plan_liberty_ship', title: 'Adquirir peças de cruzadores', goal: 'Comprar componentes para a Frota Rebelde', status: 'active' }];
  factions[2].plans = [{ id: 'plan_spice_route', title: 'Expandir rota de especiaria', goal: 'Garantir monopólio no Corredor Corelliano', status: 'active' }];
  for (const f of factions) w = upsertFaction(w, f);

  // Populate exactly 100 persistent NPCs across locations and factions
  const occupations = ['comércio', 'técnico', 'segurança', 'caçador', 'autônomo', 'piloto'];
  for (let i = 1; i <= 100; i++) {
    const locId = locations[(i - 1) % locations.length].id;
    const facId = (i % 3 === 0) ? factions[(i - 1) % factions.length].id : null;
    const occ = occupations[(i - 1) % occupations.length];
    const n = createNpc({
      id: `npc_deep_${String(i).padStart(3, '0')}`,
      name: `Habitante Galáctico ${i}`,
      locationId: locId,
      factionId: facId,
      occupation: occ,
      knownToPlayer: i <= 20,
      tags: [occ, locId.split('.')[1]]
    });
    w = upsertNpc(w, n);
  }

  w = normalizeSandboxWorld(w);
  const initialSummary = livingWorldSummary(w);
  assert.equal(initialSummary.npcCount, 100, 'Exatamente 100 NPCs devem estar registrados');

  // Adicionar memória causal local (sem telepatia):
  // Um evento na Cantina de Mos Eisley só deve ser conhecido por NPCs presentes na Cantina
  const tatooineNpc = Object.values(w.npcs).find(n => n.locationId === 'loc.tatooine.cantina');
  const monCalaNpc = Object.values(w.npcs).find(n => n.locationId === 'loc.moncala.depths');

  w = rememberNpc(w, tatooineNpc.id, {
    tick: 0,
    kind: 'local_incident',
    summary: 'Uma briga com pistoleiros rodianos quebrou a mesa da frente.',
    importance: 80,
    actor: 'bounty_hunters'
  });

  // O NPC de Tatooine deve lembrar
  const tatooineMem = w.livingDepth.npcs[tatooineNpc.id].memory;
  assert.ok(tatooineMem.some(m => /briga com pistoleiros rodianos/i.test(m.summary)));

  // O NPC de Mon Cala NÃO deve saber nada disso (ausência estrita de telepatia)
  const monCalaMem = w.livingDepth.npcs[monCalaNpc.id].memory;
  assert.equal(monCalaMem.some(m => /pistoleiros rodianos/i.test(m.summary)), false, 'NPC em Mon Cala não pode ter telepatia sobre Tatooine');

  // Simular 30 dias de campanha (120 ticks: 4 ticks por dia)
  const TICKS_TO_SIMULATE = 120;
  w.clock.tick = TICKS_TO_SIMULATE;
  w = advanceLivingWorldDepth(w, { ticks: TICKS_TO_SIMULATE });

  const postSimSummary = livingWorldSummary(w);
  assert.ok(postSimSummary.time.day >= 30, 'Simulação deve ter avançado pelo menos 30 dias');
  assert.ok(postSimSummary.alive > 0, 'População de NPCs vivos deve persistir');

  // Validar evolução offscreen dos mercados locais
  for (const loc of locations) {
    const snap = livingWorldSnapshot(w, { locationId: loc.id });
    assert.ok(snap.market, `Local ${loc.name} deve possuir perfil de mercado simulado`);
    assert.ok(snap.market.pressure >= 0 && snap.market.pressure <= 100);
    assert.ok(snap.market.supply >= 0 && snap.market.supply <= 100);
    assert.ok(snap.market.demand >= 0 && snap.market.demand <= 100);
    assert.ok(snap.weather, `Local ${loc.name} deve possuir clima gerado`);
  }

  // Validar geração e ciclo de vida do Mission Opportunity Engine
  let misGen = generateMissionOpportunities(w);
  w = misGen.world;
  assert.ok(misGen.generated >= 1, 'Motor deve gerar oportunidades a partir dos planos de facção e pressão de mercado');

  const opportunities = listMissionOpportunities(w);
  assert.ok(opportunities.length >= 1);
  const targetMission = opportunities[0];
  assert.equal(targetMission.status, MISSION_STATUS.AVAILABLE);
  assert.ok(targetMission.deadlineTick > w.clock.tick);

  // Aceitar missão e verificar registro de compromisso no patrono
  const acceptRes = acceptMissionOpportunity(w, targetMission.missionId);
  w = acceptRes.world;
  assert.equal(acceptRes.mission.status, MISSION_STATUS.ACTIVE);

  const patronState = w.livingDepth.npcs[targetMission.patronNpcId];
  assert.ok(patronState.commitments.some(c => c.text === targetMission.title), 'Compromisso da missão deve ser registrado no patrono');

  // Resolver missão com êxito
  const resolveRes = resolveMissionOpportunity(w, targetMission.missionId, { outcome: MISSION_STATUS.COMPLETED });
  w = resolveRes.world;
  assert.equal(resolveRes.mission.status, MISSION_STATUS.COMPLETED);
  const updatedPatronState = w.livingDepth.npcs[targetMission.patronNpcId];
  assert.ok(updatedPatronState.memory.some(m => m.kind === 'mission-success'), 'Patrono deve recordar o êxito da missão');

  // Testar expiração determinística de missão não aceita
  const expireMission = {
    missionId: 'mis_test_expire',
    title: 'Missão Teste Expiração',
    patronNpcId: tatooineNpc.id,
    factionId: 'fac.hutt',
    locationId: 'loc.tatooine.cantina',
    deadlineTick: w.clock.tick + 2,
    status: MISSION_STATUS.AVAILABLE
  };
  w.missionOpportunities.push(expireMission);

  // Avança o relógio além do deadline
  w.clock.tick += 5;
  const advMis = advanceMissionOpportunities(w);
  w = advMis.world;
  assert.ok(advMis.expired.includes('mis_test_expire'), 'Missão com prazo vencido deve transicionar para EXPIRED');

  // Testar 3x Recarga e serialização completa do mundo
  for (let cycle = 1; cycle <= 3; cycle++) {
    w = roundTrip(w);
    const s = livingWorldSummary(w);
    assert.equal(s.npcCount, 100, `Recarga ${cycle}: deve manter 100 NPCs`);
    assert.ok(s.time.day >= 30, `Recarga ${cycle}: deve preservar o dia da campanha`);
    const opps = listMissionOpportunities(w);
    assert.ok(opps.some(o => o.missionId === targetMission.missionId && o.status === MISSION_STATUS.COMPLETED));
    assert.ok(opps.some(o => o.missionId === 'mis_test_expire' && o.status === MISSION_STATUS.EXPIRED));
  }
});

test('Cinematic GM: Criação e execução completa de Aventura Original compilada em UAE', () => {
  let w = createWorld();
  const l1 = createLocation({ id: 'loc.narshaddaa', name: 'Setor Corelliano de Nar Shaddaa' });
  const f1 = createFaction({ id: 'fac.rebels', name: 'Célula da Aliança Rebelde' });
  const n1 = createNpc({ id: 'npc.draka', name: 'Comandante Draka', locationId: l1.id, factionId: f1.id, relation: 'ally' });
  w = upsertLocation(w, l1);
  w = upsertFaction(w, f1);
  w = upsertNpc(w, n1);
  w = normalizeSandboxWorld(w);

  // Gerar aventura original com Mestre Cinemático
  const module = createOriginalAdventure(w, {
    prompt: 'Resgatar um piloto da Aliança preso em um cassino de Nar Shaddaa',
    campaignMode: 'AOR',
    preferredPatronNpcId: n1.id,
    preferredLocationId: l1.id
  });

  assert.ok(validateOriginalAdventure(module));
  assert.equal(module.startNodeId, 'ep1-briefing');
  assert.ok(module.nodes['ep1-briefing']);
  assert.ok(module.nodes['ep2-stealth-approach']);
  assert.ok(module.nodes['ep2-direct-approach']);
  assert.ok(module.nodes['ep3-climax-sabotage']);
  assert.ok(module.nodes['ep3-climax-confrontation']);
  assert.ok(module.nodes['epilogue-victory']);
  assert.ok(module.nodes['epilogue-compromise']);

  // Executar através da UAE
  const runtime = new AdventureRuntime(module);
  assert.equal(runtime.state.currentNodeId, 'ep1-briefing');
  assert.equal(runtime.state.status, 'ACTIVE');

  // Episódio 1 -> Escolha furtiva
  runtime.transition('ep1-choice-stealth');
  assert.equal(runtime.state.currentNodeId, 'ep2-stealth-approach');
  assert.equal(runtime.state.objectives['episode-1'].status, 'COMPLETED');

  // Episódio 2 -> Infiltração para Sabotagem
  runtime.transition('ep2-stealth-to-sabotage');
  assert.equal(runtime.state.currentNodeId, 'ep3-climax-sabotage');
  assert.equal(runtime.state.objectives['episode-2'].status, 'COMPLETED');

  // Episódio 3 -> Sabotagem e Vitória
  runtime.transition('ep3-sabotage-victory');
  assert.equal(runtime.state.currentNodeId, 'epilogue-victory');
  assert.equal(runtime.state.status, 'COMPLETED');
  assert.equal(runtime.state.objectives['episode-3'].status, 'COMPLETED');
  assert.equal(runtime.state.objectives['adventure-finale'].status, 'COMPLETED');

  // 3x Round-trip reload do runtime UAE
  let serialized = runtime.serialize();
  for (let c = 1; c <= 3; c++) {
    serialized = roundTrip(serialized);
    const restored = AdventureRuntime.restore(module, serialized);
    assert.equal(restored.state.currentNodeId, 'epilogue-victory');
    assert.equal(restored.state.status, 'COMPLETED');
    assert.equal(restored.state.objectives['adventure-finale'].status, 'COMPLETED');
  }
});
