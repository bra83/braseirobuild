import {validateJewelStarportRoutes} from './jewelStarportRoutes.mjs';
import {validateJewelStarportSecurity} from './jewelStarportSecurity.mjs';
import {validateJewelEpisode3Rewards} from './jewelEpisode3Rewards.mjs';
import {validateJewelBespinDeparture} from './jewelBespinDeparture.mjs';
import {validateJewelStormIvPatrol} from './jewelStormIvPatrol.mjs';
import {validateJewelStarportCloudCars} from './jewelStarportCloudCars.mjs';
import {validateJewelShenAftermath} from './jewelShenAftermath.mjs';
import {validateJewelArendProperty,jewelArendPropertyEncounter} from './jewelArendProperty.mjs';
import {validateJewelArendPatrols,jewelArendPatrolStatus} from './jewelArendPatrols.mjs';
import {validateJewelPursuitTravel,jewelPursuitTravelStatus,jewelPursuitGroupEncounter} from './jewelPursuitTravel.mjs';
import {validateJewelResidenceWingResponse,pendingJewelResidenceWingChecks,jewelResidenceWingEncounter} from './jewelResidenceWingResponse.mjs';
import {validateJewelLandoPardon} from './jewelLandoPardon.mjs';
import {validateJewelHoloFactions,jewelHoloFactionEncounter} from './jewelHoloFactions.mjs';
import {validateJewelHoloEffects,JEWEL_HOLO_EFFECT_RULES} from './jewelHoloParkEffects.mjs';
import {validateJewelHoloNarrativeClock} from './jewelHoloNarrativeClock.mjs';
import {validateJewelHoloPark,HOLO_PARK_FACTIONS} from './jewelHoloPark.mjs';
import {jewelSlicerAvailability} from './jewelBankingOperations.mjs';
import {validateJewelArendRevenge,AREND_HOLDINGS} from './jewelArendRevenge.mjs';
import {validateJewelArrow7Chase} from './jewelArrow7Chase.mjs';
import {validateJewelArrow7Turret,ARROW7_OFFICIAL} from './jewelArrow7Turret.mjs';
import {settleJewelBank} from './jewelBankSettlement.mjs';
import {performJewelEscapeBomb,validateJewelEscapeBomb} from './jewelEscapeBomb.mjs';
import {performJewelEscapeImpact,validateJewelEscapeImpact} from './jewelEscapeImpact.mjs';
import {validateJewelCompanionImpact} from './jewelCompanionImpact.mjs';
import {replaceJewelSlicerTools,buyJewelRestrainingBolt} from './jewelBankEquipment.mjs';
import {persuadeJewelLiveDroid} from './jewelBankLiveDroid.mjs';
import {performJewelSlicingSpend} from './jewelBankSlicingSpends.mjs';
import {performJewelEvidenceAction} from './jewelBankEvidence.mjs';
import { ensureLivingWorldDepth } from '../world/livingWorldDepth.mjs';
import { influenceJewelBidder, adjudicateJewelAuctionRound, jewelAuctionStatus, adjudicateJewelGrandPrixLeg, adjudicateJewelGrandPrixPlacement, jewelGrandPrixStatus, jewelGrandPrixLegRequirements, validateJewelOperations, performJewelBankStep, performJewelBankPreparation, hireJewelSlicer } from './jewelOfYavinOperations.mjs';
export { jewelAuctionStatus, jewelGrandPrixStatus, jewelGrandPrixLegRequirements, validateJewelOperations } from './jewelOfYavinOperations.mjs';

const ID = 'jewel-of-yavin';
const SOURCE = { title: 'The Jewel of Yavin', system: 'Star Wars: Edge of the Empire', productCode: 'SWE09', userMaterial: true };
const LOC = {
  cloudCityPlaza: 'joy.loc.cloud_city_plaza',
  museumGala: 'joy.loc.museum_gala',
  vaultCorridors: 'joy.loc.vault_corridors',
  aerocarsRace: 'joy.loc.aerocars_race',
  escapePlatform: 'joy.loc.escape_platform',
  portTown: 'joy.loc.port_town',
  marketRow: 'joy.loc.market_row',
  touristDistrict: 'joy.loc.tourist_district',
  grandPrix: 'joy.loc.grand_prix',
  centralBank: 'joy.loc.central_bank'
};
const NPC = {
  a轮: 'joy.npc.arlo_serpentine',
  maris: 'joy.npc.maris_tor',
  lobot: 'joy.npc.lobot',
  aris: 'joy.npc.aris_shen',
  arend: 'joy.npc.arend_shen',
  grayson: 'joy.npc.marus_grayson',
  selis: 'joy.npc.bialar_selis'
};
const FACTION = {
  cloudCityGov: 'joy.fac.bespin_administration',
  syndicateYavin: 'joy.fac.corusca_syndicate'
};

function clone(v) { return structuredClone(v); }

/** P193: Table 2-1 patrols become source-backed combat encounters only after
 * their confirmed ETA. If the crew left the slicing location, the patrol
 * investigates there rather than teleporting into an unrelated scene.
 * IDs are derived from receipt-replayed dispatches, never supplied by a GM.
 */
const wingGuardEncounterId = (team, index) => `jewel-wing-${index}-${team.dispatchId}`;
export function jewelWingGuardInvestigations(world) {
  const p=world?.publishedAdventure;
  if(p?.id!==ID)return [];
  validateJewelOperations(world);
  const teams=p.operations?.bankSpending?.wingGuardTeams??[];
  if(!Array.isArray(teams))throw Error('Patrulhas Wing Guard inválidas.');
  const history=p.encounterHistory??[];
  if(!Array.isArray(history))throw Error('Histórico de encontros inválido.');
  const elapsed=world.clock?.elapsedMinutes??0;
  if(!Number.isSafeInteger(elapsed)||elapsed<0)throw Error('Relógio Wing Guard inválido.');
  const ids=new Set(teams.map(wingGuardEncounterId));
  if(typeof p.activeEncounterId==='string'&&p.activeEncounterId.startsWith('jewel-wing-')&&
     !ids.has(p.activeEncounterId))throw Error('Encontro Wing Guard ativo sem despacho oficial.');
  for(const r of history){
    if(typeof r?.encounterId==='string'&&r.encounterId.startsWith('jewel-wing-')&&!ids.has(r.encounterId))
      throw Error('Encontro Wing Guard sem despacho oficial.');
  }
  return teams.map((team,index)=>{
    const id=wingGuardEncounterId(team,index);
    const records=history.filter(r=>r?.encounterId===id);
    if(records.length>1)throw Error('Patrulha Wing Guard resolvida mais de uma vez.');
    if(records.length){
      const r=records[0];
      if(r.teamIndex!==index||r.dispatchId!==team.dispatchId||
        r.guardCount!==team.guardCount||r.arrivalElapsedMinutes!==team.arrivalElapsedMinutes||
        !Number.isSafeInteger(r.resolvedElapsedMinutes)||r.resolvedElapsedMinutes<team.arrivalElapsedMinutes||
        r.resolvedElapsedMinutes>elapsed||
        !['adversaries_defeated','player_incapacitated','ended','escaped','negotiated'].includes(r.outcome))
        throw Error('Resultado da patrulha Wing Guard adulterado.');
    }
    const arrived=elapsed>=team.arrivalElapsedMinutes;
    if(p.activeEncounterId===id&&!arrived)throw Error('Patrulha Wing Guard ativa antes de chegar.');
    const canIntercept=['central-bank-slice','cloud-city-pursuit'].includes(p.currentNodeId);
    const status=records.length?'resolved':p.activeEncounterId===id?'active':
      !arrived?'en_route':canIntercept?'ready':'investigating';
    if(records.length&&p.activeEncounterId===id)throw Error('Patrulha Wing Guard resolvida ainda ativa.');
    return {id,dispatchId:team.dispatchId,teamIndex:index,guardCount:team.guardCount,
      arrivalElapsedMinutes:team.arrivalElapsedMinutes,remainingMinutes:Math.max(0,team.arrivalElapsedMinutes-elapsed),
      status,arrived,investigationLocationId:LOC.centralBank,
      ...(records.length?{outcome:records[0].outcome}:{})};
  });
}
function now() { return new Date().toISOString(); }
function loc(id, name, description, tags = []) { return { id, name, description, knownToPlayer: true, tags: ['published-adventure', ID, ...tags] }; }
function npc(id, name, locationId, factionId, profile = {}) {
  return {
    id, name, locationId, factionId, goal: profile.goal || '', relation: profile.relation || 'neutral', status: 'active',
    knownToPlayer: Boolean(profile.knownToPlayer), facts: [], tags: ['published-adventure', ID, ...(profile.tags || [])],
    combatState: null, updatedAt: now(), species: profile.species || '', sex: profile.sex || '', gender: profile.gender || '',
    origin: profile.origin || '', career: profile.career || '', specialization: profile.specialization || '',
    occupation: profile.occupation || '', ageYears: profile.ageYears ?? null, appearance: profile.appearance || ''
  };
}

export const JEWEL_OF_YAVIN = Object.freeze({
  id: ID,
  title: 'A Joia de Yavin',
  originalTitle: 'The Jewel of Yavin',
  rulesProfile: 'EOTE_CORE',
  campaignModes: ['EOTE', 'MIXED'],
  source: SOURCE,
  startNodeId: 'port-town-rendezvous',
  schemaVersion: 2,
  initialClocks: {
    heistAlarm: { value: 0, max: 6, status: 'ACTIVE' },
    grandPrixPressure: { value: 0, max: 4, status: 'ACTIVE' }
  },
  implementation: { episodesImplemented: [1, 2, 3], episodesTotal: 3, boundaryNodeId: null },
  nodes: {
    'port-town-rendezvous': {
      episode: 1, title: 'Encontro em Port Town', locationId: LOC.portTown,
      summary: 'Aris Shen procura a tripulação nas ruas do porto; os homens de Vorse Tabarith não estão longe.',
      narration: 'Em Port Town, a névoa de Bespin se mistura ao vapor das oficinas. Aris chega sem escolta e examina as saídas antes de revelar quem a enviou.',
      visibleNpcIds: [NPC.aris], checks: [{skill:'Percepção',difficulty:'Média',purpose:'identificar perseguidores antes do contato'}],
      choices: [{id:'protect-aris',label:'Ajudar Aris a escapar pelo mercado',to:'market-row-chase'}]
    },
    'market-row-chase': {
      episode: 1, title: 'Fuga por Market Row', locationId: LOC.marketRow,
      summary: 'Uma perseguição entre lojas e passarelas separa Aris dos capangas de seu marido.',
      narration: 'Os corredores estreitos de Market Row se enchem de gritos. Uma porta de serviço oferece cobertura, mas também pode conduzir a um beco sem saída.',
      visibleNpcIds:[NPC.aris], checks:[{skill:'Atletismo',difficulty:'Média',purpose:'escapar dos perseguidores sem abandonar Aris'}],
      choices:[{id:'reach-tourist-district',label:'Conduzir Aris até o Distrito Turístico',to:'shen-contract'}]
    },
    'shen-contract': {
      episode:1,title:'O acordo de Arend Shen',locationId:LOC.touristDistrict,
      summary:'Arend propõe manipular o leilão, desviar o lance vencedor no Banco Central e obter a joia.',
      narration:'O anfitrião espera numa sala isolada. Aris mantém a porta sob observação enquanto o pai explica um plano que exige mais do que um ladrão habilidoso.',
      visibleNpcIds:[NPC.aris,NPC.arend],checks:[{skill:'Negociação',difficulty:'Difícil',purpose:'examinar os riscos e condições do contrato'}],
      choices:[{id:'survey-cloud-city',label:'Investigar a cidade e os preparativos do golpe',to:'bespin-arrival'}]
    },
    'heist-homework': {
      episode:1,title:'Preparação do golpe',locationId:LOC.touristDistrict,
      summary:'Investigar os licitantes, o museu e o Banco Central antes da corrida e do leilão.',
      narration:'Os dossiês de concorrentes se acumulam sobre a mesa. Uma falha no planejamento pode expor o grupo antes da primeira oferta.',
      visibleNpcIds:[NPC.aris],checks:[{skill:'Manha',difficulty:'Média',purpose:'identificar interesses dos licitantes'}],
      choices:[{id:'prepare-grand-prix',label:'Preparar veículo e equipe para o Grande Prêmio',to:'grand-prix-preparation'}]
    },
    'grand-prix-preparation': {
      episode:1,title:'Oficina e credenciais',locationId:LOC.grandPrix,
      summary:'A corrida é uma via de acesso à festa; credenciais, mecânicos e o veículo precisam de atenção.',
      narration:'Nos boxes, as turbinas vibram em marcha lenta. Rivais observam a equipe enquanto os fiscais conferem registros e números de competição.',
      visibleNpcIds:[],checks:[{skill:'Mecânica',difficulty:'Média',purpose:'preparar o cloud car para a prova'}],
      choices:[{id:'take-race-start',label:'Largar no Grande Prêmio de Cloud City',to:'grand-prix-race'}]
    },
    'grand-prix-race': {
      episode:1,title:'Grande Prêmio de Cloud City',locationId:LOC.grandPrix,
      summary:'O piloto enfrenta adversários e obstáculos sobre os níveis de Bespin; o resultado fica registrado.',
      narration:'O ar gelado atravessa a cabine quando o sinal da largada desaparece. Os veículos rivais mergulham entre as plataformas de mineração.',
      visibleNpcIds:[],checks:[{skill:'Pilotagem (Planetária)',difficulty:'Difícil',purpose:'obter colocação suficiente para a recepção'}],
      choices:[{id:'race-aftermath',label:'Encerrar a corrida e seguir ao círculo de vencedores',to:'grand-prix-aftermath'}]
    },
    'grand-prix-aftermath': {
      episode:1,title:'Círculo dos vencedores',locationId:LOC.grandPrix,
      summary:'A equipe apura a classificação e procura uma entrada legítima ou alternativa para a festa.',
      narration:'A multidão vibra junto ao pódio. A festa continua, mas a posição na corrida altera quem recebe os convites e quem desperta suspeitas.',
      visibleNpcIds:[NPC.aris],checks:[{skill:'Charme',difficulty:'Média',purpose:'negociar acesso ao evento após a corrida'}],
      choices:[{id:'go-to-museum',label:'Ir ao museu para o leilão',to:'gala-reconnaissance'}]
    },
    'cloud-nine-party': {
      episode:2,title:'Recepção no Cloud Nine',locationId:LOC.museumGala,
      summary:'Os licitantes disputam a joia; a equipe precisa criar pressão de preço sem revelar o plano.',
      narration:'As conversas cessam quando o brilho da gema surge na vitrine. Marus Grayson observa a plateia, atento a cada oferta.',
      visibleNpcIds:[NPC.grayson],checks:[{skill:'Enganação',difficulty:'Difícil',purpose:'estimular a disputa sem denunciar a manipulação'}],
      choices:[{id:'bank-operation',label:'Executar a operação bancária durante o leilão',to:'central-bank-slice'}]
    },
    'central-bank-slice': {
      episode:2,title:'Invasão ao Banco Central',locationId:LOC.centralBank,
      summary:'Desviar a transferência do lance vencedor exige um teste de Computadores adjudicado.',
      narration:'Linhas de autenticação percorrem o terminal. Um desvio prematuro deixa uma trilha rastreável para a segurança financeira de Bespin.',
      visibleNpcIds:[],checks:[{skill:'Computadores',difficulty:'Difícil',purpose:'desviar a transferência do leilão'}],
      choices:[
        {id:'skip-bank-transfer',label:'Abandonar a transferência e priorizar a joia',to:'vault-infiltration'},
        {id:'divert-winning-bid',label:'Confirmar desvio bancário bem-sucedido',to:'vault-infiltration',condition:'flag:bankDiversionSecured',effects:{bankDiversionSecured:true}}
      ]
    },
    'escape-without-jewel': {
      episode:3,title:'Retirada sem a joia',locationId:LOC.escapePlatform,
      summary:'O golpe não recuperou a joia. O grupo pode fugir sem atribuir a si recompensas não obtidas.',
      narration:'Os corredores da plataforma ficam para trás. A joia permanece em Bespin, e as decisões tomadas ainda terão consequências.',
      visibleNpcIds:[],checks:[],choices:[]
    },
    'bespin-arrival': {
      episode: 1,
      title: 'Chegada a Cloud City',
      locationId: LOC.cloudCityPlaza,
      summary: 'O grupo chega à magnífica Cidade das Nuvens para planejar o maior roubo de joia da galáxia.',
      narration: 'As torres reluzentes de Cloud City flutuam sobre as nuvens cor-de-laranja de Bespin. O leilão da lendária Joia de Yavin atrai magnatas, apostadores e ladrões de elite.',
      visibleNpcIds: [],
      checks: [
        { skill: 'Manha', difficulty: 'Média', purpose: 'identificar os círculos da alta sociedade e sistemas de vigilância' },
        { skill: 'Enganação', difficulty: 'Média', purpose: 'assumir identidades falsas no controle de entrada' }
      ],
      choices: [
        { id: 'prepare-heist', label: 'Levantar dados dos licitantes e do Grande Prêmio', to: 'heist-homework' }
      ]
    },
    'gala-reconnaissance': {
      episode: 1,
      title: 'A Gala de Exibição',
      locationId: LOC.museumGala,
      summary: 'Reconhecimento das defesas do cofre e socialização com os principais patrocinadores e seguranças.',
      narration: 'Músicos tocam melodias refinadas enquanto taças de néctar cintilam sob lustres suspensos. No centro do salão blindado, o corusca gem de proporções inacreditáveis repousa sob um pedestal magnético.',
      visibleNpcIds: [NPC.maris],
      checks: [
        { skill: 'Charme', difficulty: 'Difícil', purpose: 'conquistar a confiança de figuras-chave do leilão' },
        { skill: 'Computadores', difficulty: 'Difícil', purpose: 'sondar o circuito de segurança da cúpula' }
      ],
      choices: [
        { id: 'attend-cloud-nine', label: 'Acompanhar a recepção e o leilão', to: 'cloud-nine-party' }
      ]
    },
    'vault-infiltration': {
      episode: 2,
      title: 'Infiltração no Cofre Central',
      locationId: LOC.vaultCorridors,
      summary: 'Execução do assalto durante o Grande Prêmio de Cloud City.',
      narration: 'O rugido dos motores do Grand Prix abafa os sons de corte térmico e desativação de sensores nos dutos do museu. A equipe avança contra o cronômetro para contornar o cofre.',
      visibleNpcIds: [],
      checks: [
        { skill: 'Falcatrua', difficulty: 'Difícil', purpose: 'desarmar as travas magnéticas do cofre' },
        { skill: 'Furtividade', difficulty: 'Difícil', purpose: 'evitar os droides de patrulha da administração' }
      ],
      choices: [
        { id: 'retreat-without-jewel', label: 'Retirar-se sem a joia', to: 'cloud-city-pursuit' },
        { id: 'take-jewel', label: 'Retirar a joia após destravar o cofre', to: 'cloud-city-pursuit', condition:'check-success:vault-infiltration', effects: { jewelAcquired: true } }
      ]
    },
    'cloud-city-pursuit': {
      episode: 2,
      title: 'Perseguição nas Nuvens',
      locationId: LOC.aerocarsRace,
      summary: 'Fuga em alta velocidade com aerocarros entre as estruturas suspensas de Bespin.',
      narration: 'Alarmes finalmente ecoam pelas torres. Veículos de segurança cortam o céu enquanto a tripulação acelera o speeder de fuga entre as turbinas e refinarias de gás tibanna.',
      visibleNpcIds: [],
      checks: [
        { skill: 'Pilotagem (Planetária)', difficulty: 'Difícil', purpose: 'costurar entre os cânions de edifícios flutuantes' },
        { skill: 'Artilharia', difficulty: 'Média', purpose: 'neutralizar os motores das naves persecutórias' }
      ],
      choices: [
        { id: 'reach-hangar-empty', label: 'Escapar sem a joia', to: 'escape-without-jewel', condition:'!flag:jewelAcquired' },
        { id: 'reach-hangar', label: 'Alcançar a plataforma com a joia', to: 'jewel-epilogue', condition:'flag:bespinEscapeVerified' }
      ]
    },
    'jewel-epilogue': {
      episode: 3,
      title: 'Epílogo: O Golpe do Século',
      locationId: LOC.escapePlatform,
      summary: 'A fuga bem-sucedida de Bespin com a Joia de Yavin e a repartição dos créditos.',
      narration: 'A nave rasga a atmosfera superior de Bespin e entra no hiperespaço. Com a Joia de Yavin em mãos, a tripulação consolida sua lenda no submundo da galáxia.',
      visibleNpcIds: [],
      checks: [],
      choices: []
    }
  }
});

function conditionOk(choice, progress) {
  const c = choice?.condition;
  if (c == null) return true;
  if (typeof c !== 'string') return false;
  if (c.startsWith('unvisited:')) return !progress.visitedNodeIds.includes(c.slice(10));
  if (c.startsWith('visited:')) return progress.visitedNodeIds.includes(c.slice(8));
  if (c.startsWith('flag:')) return progress.flags?.[c.slice(5)] === true;
  if (c.startsWith('!flag:')) return progress.flags?.[c.slice(6)] !== true;
  if (c.startsWith('check-success:')) {
    const nodeId = c.slice(14);
    if (!Object.hasOwn(JEWEL_OF_YAVIN.nodes, nodeId)) return false;
    return (progress.checkHistory || []).some(e => e.nodeId === nodeId && e.success === true);
  }
  return false;
}

function playerNode(node, progress) {
  if (!node) return null;
  return {
    id: progress.currentNodeId,
    episode: node.episode,
    title: node.title,
    locationId: node.locationId,
    summary: node.summary,
    narration: node.narration,
    visibleNpcIds: [...(node.visibleNpcIds || [])],
    checks: clone(node.checks || []),
    choices: (node.choices || []).filter(c => conditionOk(c, progress)).map(c => ({ id: c.id, label: c.label }))
  };
}

function baseProgress({ rewardsApplied = false } = {}) {
  return {
    id: ID,
    title: JEWEL_OF_YAVIN.title,
    status: 'ACTIVE',
    currentNodeId: JEWEL_OF_YAVIN.startNodeId,
    visitedNodeIds: [JEWEL_OF_YAVIN.startNodeId],
    flags: {},
    bankContractVersion: 1,
    bankEquipmentContractVersion: 1,
    clocks: clone(JEWEL_OF_YAVIN.initialClocks),
    checkHistory: [],
    consequencesApplied: false,
    startedAt: now(),
    updatedAt: now(),
    completedAt: null,
    rewardsApplied: Boolean(rewardsApplied),
    source: SOURCE,
    implementation: clone(JEWEL_OF_YAVIN.implementation)
  };
}

export function startJewelOfYavin(world) {
  const next = clone(world || {});
  next.publishedAdventure = baseProgress();
  const p = next.publishedAdventure;
  return {
    world: next,
    progress: clone(p),
    node: playerNode(JEWEL_OF_YAVIN.nodes[p.currentNodeId], p)
  };
}

export function advanceJewelOfYavin(world, choiceId) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');
  const node = JEWEL_OF_YAVIN.nodes[p.currentNodeId];
  validateJewelOperations(next);
  validateJewelCompanionImpact(p);
  validateJewelArrow7Turret(p);
  validateJewelArrow7Chase(p);
  validateJewelArendRevenge(p);
  validateJewelLandoPardon(p);
  validateJewelShenAftermath(p);
  validateJewelArendProperty(p);
  validateJewelArendPatrols(p,world);
  validateJewelResidenceWingResponse(p,world);
  validateJewelBespinDeparture(p,world);
  validateJewelStarportSecurity(p,world);
  validateJewelStarportRoutes(p,world);
  validateJewelEpisode3Rewards(p);
  validateJewelHoloPark(p);
  validateJewelHoloEffects(p);
  validateJewelHoloFactions(p);
  const pendingWing=jewelWingGuardInvestigations(next).find(team=>team.status==='ready'||team.status==='active');
  if(p.activeEncounterId||pendingWing)throw Error('Resolva o encontro Wing Guard ou combate ativo antes de avançar a aventura.');
  if(p.currentNodeId==='cloud-city-pursuit'&&p.operations?.escapeBomb){
    const bomb=validateJewelEscapeBomb(p),impact=validateJewelEscapeImpact(p);
    if(bomb.blastDamageBeforeSoak&&!impact?.blastApplied)throw Error('Aplique a explosão na ficha antes de avançar.');
    if(bomb.fallDamagePending&&!impact?.fallApplied)throw Error('Resolva a queda na ficha antes de avançar.');
    if(!p.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome))
      throw Error('Resolva o encontro oficial com os quatro capangas de Arend antes de encerrar a fuga.');
  }
  const choice = (node?.choices || []).find(c => c.id === choiceId);
  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha inválida.');
  // P185: once a GM starts the source-adjudicated operation, it cannot be
  // silently skipped by advancing the scene. Legacy saves with no operation
  // retain their pre-P184 routes and are never assigned phantom results.
  if (p.currentNodeId === 'grand-prix-race' && p.operations?.race &&
      (!p.operations.race.completed || !p.operations.race.placement))
    throw new Error('Conclua os sete trechos e a classificação do Grande Prêmio antes de avançar.');
  if (p.currentNodeId === 'cloud-city-pursuit' && p.operations?.escapeBomb &&
      !['DISARMED','JUMPED','JUMPED_WITH_JEWEL','JEWEL_DROPPED','FELL','EXPLODED'].includes(p.operations.escapeBomb.status))
    throw new Error('Resolva a armadilha do airspeeder iniciada antes de encerrar a fuga.');
  if (p.currentNodeId === 'cloud-nine-party' && p.operations?.auction &&
      !p.operations.auction.completed)
    throw new Error('Conclua o leilão iniciado antes de avançar para o banco.');
  p.flags = { ...(p.flags || {}), ...(choice.effects || {}) };
  p.currentNodeId = choice.to;
  if (!p.visitedNodeIds.includes(choice.to)) p.visitedNodeIds.push(choice.to);
  const target = JEWEL_OF_YAVIN.nodes[choice.to];
  if (!target.choices || target.choices.length === 0) {
    p.status = 'COMPLETED';
    p.completedAt = now();
  }
  p.updatedAt = now();
  next.publishedAdventure = p;
  return {
    world: next,
    progress: clone(p),
    node: playerNode(target, p),
    completed: p.status === 'COMPLETED'
  };
}

export function jewelOfYavinStatus(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) {
    return {
      available: true,
      active: false,
      catalog: { id: ID, title: JEWEL_OF_YAVIN.title, campaignModes: [...JEWEL_OF_YAVIN.campaignModes], rulesProfile: JEWEL_OF_YAVIN.rulesProfile }
    };
  }
  validateJewelOperations(world);
  validateJewelEscapeBomb(p);
  validateJewelEscapeImpact(p);
  validateJewelCompanionImpact(p);
  validateJewelArrow7Turret(p);
  validateJewelArrow7Chase(p);
  validateJewelArendRevenge(p);
  validateJewelLandoPardon(p);
  validateJewelShenAftermath(p);
  validateJewelArendProperty(p);
  validateJewelArendPatrols(p,world);
  validateJewelResidenceWingResponse(p,world);
  validateJewelBespinDeparture(p,world);
  validateJewelStarportSecurity(p,world);
  validateJewelStarportRoutes(p,world);
  validateJewelEpisode3Rewards(p);
  validateJewelHoloPark(p);
  validateJewelHoloEffects(p);
  const node = JEWEL_OF_YAVIN.nodes[p.currentNodeId];
  return {
    available: true,
    active: p.status === 'ACTIVE',
    completed: p.status === 'COMPLETED',
    archived: p.status === 'ARCHIVED',
    progress: clone(p),
    node: playerNode(node, p),
    // P185: visible to the existing Android publishedAdventureStatus bridge.
    // A legacy save without a race operation still receives source guidance.
    bankStatus: p.operations?.bank ? clone(p.operations.bank) : null,
    bankSettlement: p.operations?.bankSettlement ? clone(p.operations.bankSettlement) : null,
    escapeBomb: p.operations?.escapeBomb ? clone(p.operations.escapeBomb) : null,
    escapeImpact: p.operations?.escapeImpact ? clone(p.operations.escapeImpact) : null,
    escapeCompanionImpact: p.operations?.escapeCompanionImpact ? clone(p.operations.escapeCompanionImpact) : null,
    arrow7Turret: p.operations?.arrow7Turret ? clone(p.operations.arrow7Turret) : null,
    arrow7Chase: p.operations?.arrow7Chase ? clone(p.operations.arrow7Chase) : null,
    arendRevenge: p.operations?.arendRevenge ? clone(p.operations.arendRevenge) : null,
    landoPardon: clone(validateJewelLandoPardon(p)),
    shenAftermath: clone(validateJewelShenAftermath(p)),
    arendProperty: clone(validateJewelArendProperty(p)),
    arendPatrols: clone(jewelArendPatrolStatus(world)),
    residenceWingResponse: clone(validateJewelResidenceWingResponse(p,world)),
    residenceWingPending: clone(pendingJewelResidenceWingChecks(p)),
    pursuitTravel: clone(jewelPursuitTravelStatus(world)),
    episode3Rewards: clone(validateJewelEpisode3Rewards(p)),
    bespinDeparture: clone(validateJewelBespinDeparture(p,world)),
    starportSecurity: clone(validateJewelStarportSecurity(p,world)),
    starportRoutes: clone(validateJewelStarportRoutes(p,world)),
    stormIvPatrol: clone(validateJewelStormIvPatrol(p)),
    starportCloudCars: clone(validateJewelStarportCloudCars(p,world)),
    holoPark: p.operations?.holoPark ? clone(p.operations.holoPark) : null,
    holoNarrativeClock: clone(validateJewelHoloNarrativeClock(p,world)),
    holoEffects: p.operations?.holoEffects ? clone(p.operations.holoEffects) : null,
    holoFactions: clone(validateJewelHoloFactions(p)),
    holoEffectRules: clone(JEWEL_HOLO_EFFECT_RULES),
    holoParkFactions: clone(HOLO_PARK_FACTIONS),
    arendHoldings: p.currentNodeId==='cloud-city-pursuit'?clone(AREND_HOLDINGS):null,
    arrow7Vehicle: p.currentNodeId==='cloud-city-pursuit'?clone(ARROW7_OFFICIAL):null,
    bankSpending: p.operations?.bankSpending ? clone(p.operations.bankSpending) : null,
    bankTools: p.operations?.bankTools ? clone(p.operations.bankTools) : null,
    bankEvidence: p.operations?.bankEvidence ? clone(p.operations.bankEvidence) : null,
    liveDroid: p.operations?.liveDroid ? clone(p.operations.liveDroid) : null,
    wingGuardInvestigations: jewelWingGuardInvestigations(world),
    encounter: currentJewelOfYavinEncounter(world),
    hiredSlicer: p.operations?.hiredSlicer ? clone(p.operations.hiredSlicer) : null,
    slicerAvailability: jewelSlicerAvailability(p),
    bankPreparation: p.operations?.bankPreparation ? clone(p.operations.bankPreparation) : null,
    raceNextLegRequirements: p.status === 'ACTIVE' && p.currentNodeId === 'grand-prix-race' &&
      !p.operations?.race?.completed
      ? jewelGrandPrixLegRequirements((p.operations?.race?.leg ?? 0) + 1, p.operations?.race)
      : null
  };
}

export function currentJewelOfYavinScene(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) return null;
  const node = JEWEL_OF_YAVIN.nodes[p.currentNodeId];
  if (!node) return null;
  return { locationId: node.locationId, title: node.title, narration: node.narration, visibleNpcIds: [...(node.visibleNpcIds || [])] };
}

export function currentJewelOfYavinChecks(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') return [];
  const node = JEWEL_OF_YAVIN.nodes[p.currentNodeId];
  return clone(node?.checks || []);
}

export function adjudicateJewelOfYavinCheck(world, { checkIndex = 0, result = {} } = {}) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');
  const checks = currentJewelOfYavinChecks(next);
  const check = checks[checkIndex];
  if (!check) throw new Error('Teste inválido.');
  if (p.currentNodeId === 'central-bank-slice' && p.bankContractVersion === 1)
    throw new Error('Use a operação bancária com droid G-100 e dados reais; o teste narrativo não transfere fundos.');
  if (typeof result.success !== 'boolean') throw new Error('Resultado de teste deve indicar sucesso verdadeiro ou falso.');
  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, success: result.success };
  if (!entry.success && ['grand-prix-race','central-bank-slice','vault-infiltration'].includes(p.currentNodeId)) {
    const clock = p.clocks.heistAlarm;
    clock.value = Math.min(clock.max, clock.value + 1);
    clock.status = clock.value >= clock.max ? 'COMPLETED' : 'ACTIVE';
  }
  p.checkHistory.push(entry);
  p.updatedAt = now();
  next.publishedAdventure = p;
  return { world: next, progress: clone(p), adjudication: entry };
}

export function currentJewelOfYavinMasterRequest(world) {
  const s = jewelOfYavinStatus(world);
  if (!s.active && !s.completed) return null;
  const node = s.node;
  if (!node) return null;
  return {
    kind: 'published_adventure',
    adventureId: ID,
    adventureTitle: JEWEL_OF_YAVIN.title,
    nodeId: node.id,
    episode: node.episode,
    locationId: node.locationId,
    summary: node.summary,
    checks: currentJewelOfYavinChecks(world),
    suggestedActions: (node.choices || []).map(c => c.label),
    antiSpoiler: true,
    source: SOURCE
  };
}

export const performJewelOfYavinEscapeImpact = performJewelEscapeImpact;
export function currentJewelOfYavinEncounter(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') return null;
  // P225: physical Wing Guard interception has priority once a failed
  // 15-minute patrol is explicitly confirmed. Existing combat remains active.
  for(const response of validateJewelResidenceWingResponse(p)){
    const encounter=jewelResidenceWingEncounter(p,response.encounterId);
    if(encounter)return encounter;
  }
  // P226: GM-confirmed physically present groups after a real location-travel check.
  for(const response of validateJewelPursuitTravel(p).arrivals){
    const encounter=jewelPursuitGroupEncounter(p,response.encounterId);
    if(encounter)return encounter;
  }
  // P220: separately prepared property stages are not auto-instantiated.
  const property=validateJewelArendProperty(p);
  for(const stage of property?.stages??[]){
    const ready=jewelArendPropertyEncounter(p,stage.encounterId);
    if(ready)return ready;
  }
  const wing=jewelWingGuardInvestigations(world);
  const activeWing=wing.find(t=>t.status==='active');
  const readyWing=wing.find(t=>t.status==='ready');
  const patrol=activeWing??readyWing;
  if(patrol)return {id:patrol.id,label:`Guarda Alada de Bespin — equipe ${patrol.teamIndex+1}`,
    available:true,status:patrol.status,guardCount:patrol.guardCount,
    preparation:'Tabela 2-1 SWE09: patrulha chegou após o rastreamento; combate, negociação ou fuga devem ser adjudicados.'};
  const history = p.encounterHistory || [];
  let id = null, label = null;
  if(p.currentNodeId==='cloud-city-pursuit' && p.operations?.escapeBomb){
    const bomb=validateJewelEscapeBomb(p),impact=validateJewelEscapeImpact(p);
    const terminal=['DISARMED','JUMPED','JUMPED_WITH_JEWEL','JEWEL_DROPPED','FELL','EXPLODED'].includes(bomb?.status);
    const injuriesReady=(!bomb?.blastDamageBeforeSoak||impact?.blastApplied)&&(!bomb?.fallDamagePending||impact?.fallApplied);
    if(terminal&&injuriesReady&&!history.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome)){
      id='arend-shen-four-goons';label='Quatro capangas de Arend Shen — airspeeder Arrow-7';
    }
  }
  if (p.currentNodeId === 'cloud-city-pursuit' && !id && !p.operations?.escapeBomb) {
    id = 'bespin-security-intercept';
    label = 'Interceptação da Segurança de Bespin';
  }
  // SWE09 p.94: the starport interception is two minion groups of two
  // Wing Guard officers and one sergeant. This is a NEW encounter after
  // the Arrow-7 scene, never the bank investigation patrol from Table 2-1.
  if(p.currentNodeId==='cloud-city-pursuit' &&
     history.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome)&&
     (p.activeEncounterId==='holo-park-wing-guard'||!p.operations?.holoPark)&&
     !history.some(x=>x.encounterId==='holo-park-wing-guard'&&x.outcome)){
    id='holo-park-wing-guard';label='Grand Galaxy Holo-Park — dois grupos de guardas e sargento';
  }
  if(!id){
    for(const candidate of ['holo-park-arend-goons','holo-park-grayson-security']){
      const faction=jewelHoloFactionEncounter(p,candidate);if(faction)return faction;
    }
  }
  if (!id) return null;
  const resolved = history.some(x => x.encounterId === id && x.outcome);
  return { id, label, available: !resolved, status: resolved ? 'resolved' : (p.activeEncounterId === id ? 'active' : 'ready') };
}

export function jewelOfYavinEncounterSpec(world, encounterId = null) {
  const current = currentJewelOfYavinEncounter(world);
  const id = String(encounterId || current?.id || '');
  const patrol=jewelWingGuardInvestigations(world).find(team=>team.id===id);
  if(patrol){
    if(current?.id!==id||!['ready','active'].includes(patrol.status))
      throw Error('Patrulha Wing Guard não chegou, foi resolvida ou está em outra cena.');
    // Table 2-1 specifically says a team EQUAL to the number of PCs.
    // The standard four-officer-plus-sergeant patrol on printed p.12 is
    // a different encounter, not a reason to add an invented sergeant here.
    return {id,label:`Guarda Alada de Bespin — equipe ${patrol.teamIndex+1}`,
      playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
      adversaries:[{profileKey:'bespin_wing_guard_officer',count:patrol.guardCount,range:'Médio'}]};
  }
  if(id==='holo-park-wing-guard'&&current?.id===id&&current.available){
    return {id,label:'Grand Galaxy Holo-Park — dois grupos de guardas e sargento',
      playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
      adversaries:[
        {profileKey:'bespin_wing_guard_officer',count:2,range:'Médio',name:'Guarda Alada — grupo 1'},
        {profileKey:'bespin_wing_guard_officer',count:2,range:'Médio',name:'Guarda Alada — grupo 2'},
        {profileKey:'bespin_wing_guard_sergeant',count:1,range:'Médio',name:'Sargento da Guarda Alada'}
      ]};
  }
  const pursuit=jewelPursuitGroupEncounter(world?.publishedAdventure,id);
  if(pursuit&&current?.id===id&&current.available)return pursuit;
  const residence=jewelResidenceWingEncounter(world?.publishedAdventure,id);
  if(residence&&current?.id===id&&current.available)return residence;
  const property=jewelArendPropertyEncounter(world?.publishedAdventure,id);
  if(property&&current?.id===id&&current.available)return property;
  const faction=jewelHoloFactionEncounter(world?.publishedAdventure,id);
  if(faction&&current?.id===id&&current.available)return faction;
  if(id==='arend-shen-four-goons'&&current?.id===id&&current.available){
    return {id,label:'Quatro capangas de Arend Shen — airspeeder Arrow-7',
      playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
      adversaries:[{profileKey:'shens_goons',count:4,range:'Médio'}]};
  }
  if (id === 'bespin-security-intercept' && current?.id===id && current.available) {
    return {
      id,
      label: 'Agentes de Segurança de Bespin',
      playerInitiativeSkill: 'alerta',
      npcInitiativeSkill: 'alerta',
      adversaries: [
        { profileKey: 'bespin_security_officer', count: 3, range: 'Médio' },
        { profileKey: 'arlo_serpentine', range: 'Longo' }
      ]
    };
  }
  throw new Error('Nenhum encontro de combate de A Joia de Yavin neste ponto.');
}

export function markJewelOfYavinEncounterActive(world, encounterId) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não ativa.');
  const current = currentJewelOfYavinEncounter(next);
  if (p.activeEncounterId || !current || current.id !== encounterId || !current.available || current.status !== 'ready')
    throw new Error('Encontro incompatível com a cena atual ou já resolvido.');
  p.activeEncounterId = encounterId;
  p.updatedAt = now();
  next.publishedAdventure = p;
  return next;
}

export function recordJewelOfYavinEncounterOutcome(world, { encounterId, outcome, adversaries = [] } = {}) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não ativa.');
  const current = currentJewelOfYavinEncounter(next);
  if (!current || current.id !== encounterId || !current.available || p.activeEncounterId !== encounterId)
    throw new Error('Encontro incompatível, não iniciado ou já resolvido.');
  if (!['adversaries_defeated','player_incapacitated','ended','escaped','negotiated'].includes(outcome))
    throw new Error('Resultado de encontro inválido.');
  p.encounterHistory = Array.isArray(p.encounterHistory) ? p.encounterHistory : [];
  const wing=jewelWingGuardInvestigations(next).find(t=>t.id===encounterId);
  const record = { at: now(), nodeId: p.currentNodeId, encounterId, outcome,
    ...(wing?{teamIndex:wing.teamIndex,dispatchId:wing.dispatchId,
      guardCount:wing.guardCount,arrivalElapsedMinutes:wing.arrivalElapsedMinutes,
      resolvedElapsedMinutes:next.clock?.elapsedMinutes??0}:{}) };
  p.encounterHistory.push(record);
  p.activeEncounterId = null;
  if(!wing)p.flags = { ...(p.flags || {}), securityEvaded: outcome === 'adversaries_defeated' };
  else p.flags={...(p.flags||{}),wingGuardLastOutcome:outcome};
  jewelWingGuardInvestigations(next); // receipt-backed, reject forged resolution before commit

  p.updatedAt = now();
  next.publishedAdventure = p;
  return { world: next, record };
}

export function markJewelOfYavinRewardsApplied(world, details = {}) {
  const next = clone(world || {});
  next.publishedAdventureRewards = { ...(next.publishedAdventureRewards || {}) };
  next.publishedAdventureRewards[ID] = {
    applied: true,
    appliedAt: now(),
    credits: Number.isFinite(Number(details.credits)) ? Number(details.credits) : 0,
    obligationReduced: Number.isFinite(Number(details.obligationReduced)) ? Number(details.obligationReduced) : 0,
    note: details.note || 'Desfecho da Joia de Yavin: valores dependem de adjudicação e recibos'
  };
  if (next?.publishedAdventure?.id === ID) {
    next.publishedAdventure.rewardsApplied = true;
    next.publishedAdventure.updatedAt = now();
  }
  return next;
}

export function archiveJewelOfYavin(world) {
  const next = clone(world || {});
  const p = next?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'COMPLETED') {
    throw new Error('A Joia de Yavin precisa estar concluída antes de arquivar.');
  }
  p.status = 'ARCHIVED';
  p.archivedAt = now();
  p.updatedAt = now();
  next.publishedAdventure = p;
  return next;
}


// P184: optional source-based Grand Prix and auction adjudication; legacy saves remain playable.
export const influenceJewelOfYavinBidder=(world,details)=>influenceJewelBidder(world,JEWEL_OF_YAVIN.nodes,details);
export const adjudicateJewelOfYavinAuctionRound=(world,details)=>adjudicateJewelAuctionRound(world,JEWEL_OF_YAVIN.nodes,details);
export const adjudicateJewelOfYavinRaceLeg=(world,details)=>adjudicateJewelGrandPrixLeg(world,JEWEL_OF_YAVIN.nodes,details);
export const adjudicateJewelOfYavinRacePlacement=(world,details)=>adjudicateJewelGrandPrixPlacement(world,JEWEL_OF_YAVIN.nodes,details);

// P200: account-to-account bank ledger, no character credits awarded.
export const settleJewelOfYavinBank=(world,details)=>settleJewelBank(world,details);
export const performJewelOfYavinEscapeBomb=(world,character,details)=>performJewelEscapeBomb(world,character,details);

// P187: full G-100 remote banking droid flow with real opposed dice and receipt replay.
export const performJewelOfYavinBankSpending=(world,details,character=null,partySize=1)=>performJewelSlicingSpend(world,details,character,partySize);
export const buyJewelOfYavinRestrainingBolt=(world,character,details)=>buyJewelRestrainingBolt(world,character,details);
export const replaceJewelOfYavinSlicerTools=(world,character,details)=>replaceJewelSlicerTools(world,character,details);
export const performJewelOfYavinBankStep=(world,character,details)=>performJewelBankStep(world,JEWEL_OF_YAVIN.nodes,character,details);
export const performJewelOfYavinLiveDroid=(world,character,details)=>persuadeJewelLiveDroid(world,character,details);
export const performJewelOfYavinBankEvidence=(world,character,details)=>performJewelEvidenceAction(world,character,details);
export const performJewelOfYavinBankPreparation=(world,character,details)=>performJewelBankPreparation(world,character,details);

/** P187 migration: a legacy successful bank check is evidence, not a fabricated transfer.
 * Preserve it as the old engine's authorized flag without rewriting UAE history.
 */
export function migrateJewelOfYavinWorld(world){
  const p=world?.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.bankContractVersion===1||
     p.flags?.bankDiversionSecured===true||
     !(p.checkHistory||[]).some(e=>e.nodeId==='central-bank-slice'&&e.success===true))return world;
  const next=clone(world);
  next.publishedAdventure.flags={...(next.publishedAdventure.flags||{}),bankDiversionSecured:true};
  return next;
}

export const hireJewelOfYavinSlicer=(world,character,details)=>hireJewelSlicer(world,character,details);
