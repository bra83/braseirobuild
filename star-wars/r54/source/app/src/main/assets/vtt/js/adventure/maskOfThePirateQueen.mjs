import { ensureLivingWorldDepth } from '../world/livingWorldDepth.mjs';

const ID = 'mask-of-the-pirate-queen';
const SOURCE = { title: 'Mask of the Pirate Queen', system: 'Star Wars: Edge of the Empire', productCode: 'SWE13', userMaterial: true };
const LOC = {
  saleucamiPort:'mpq.loc.taleucema',paradise:'mpq.loc.paradise',krezoShop:'mpq.loc.krezo_shop',
  blackwind:'mpq.loc.blackwind_crater',vault:'mpq.loc.vault',worlport:'mpq.loc.worlport',
  vandinCasino:'mpq.loc.vandin_casino',rustyReach:'mpq.loc.rusty_reach',
  queenPalace:'mpq.loc.queen_palace',spaceBattle:'mpq.loc.renegades_blood_orbit',
  shipCorridors:'mpq.loc.renegades_blood_corridors',flagshipBridge:'mpq.loc.flagship_bridge',
  sororinet:'mpq.loc.sororinet_node'
};
const NPC = {
  venlana:'mpq.npc.venlana_sipal',graf:'mpq.npc.graf',krezo:'mpq.npc.krezo_wasanti',
  revah:'mpq.npc.revah',jander:'mpq.npc.jander',illo:'mpq.npc.illo_vandin',
  ryale:'mpq.npc.ryale_wei',noira:'mpq.npc.noira',sykes:'mpq.npc.jerid_sykes',
  porel:'mpq.npc.porel_vakra',lathe:'mpq.npc.lathe'
};
const FACTION = { veiledSorority:'mpq.fac.veiled_sorority',zannConsortium:'mpq.fac.zann_consortium' };

function clone(v) { return structuredClone(v); }
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

export const MASK_OF_THE_PIRATE_QUEEN = Object.freeze({
  id: ID,
  title: 'A Máscara da Rainha Pirata',
  originalTitle: 'Mask of the Pirate Queen',
  rulesProfile: 'EOTE_CORE',
  campaignModes: ['EOTE', 'MIXED'],
  source: SOURCE,
  startNodeId: 'saleucami-contract-offer',
  schemaVersion: 3,
  initialClocks: {
    pirateAlert: { value: 0, max: 6, status: 'ACTIVE' }
  },
  implementation: { episodesImplemented: [1, 2, 3], episodesTotal: 3, boundaryNodeId: null },
  nodes: {
    "saleucami-contract-offer": {
      episode: 1,
      title: "Contrato da Irmandade Oculta",
      summary: "O grupo recebe um contato seguro de Venlana Sipal, do Consórcio Zann, propondo uma caçada à Rainha Pirata.",
      narration: "O grupo recebe um contato seguro de Venlana Sipal, do Consórcio Zann, propondo uma caçada à Rainha Pirata.",
      checks: [
            {
                  skill: "Negociação",
                  difficulty: "Média",
                  purpose: "esclarecer condições do adiantamento e a intermediação do contrato"
            }
      ],
      choices: [
            {
                  id: "accept-venlana",
                  label: "Aceitar conversar em Saleucami",
                  to: "taleucema-arrival",
                  effects: {
                        contractAccepted: true,
                        contractRetainerAccepted: true
                  }
            }
      ],
      sourceSection: "Episode I, pp. 16-17",
      locationId: LOC.saleucamiPort,
      visibleNpcIds: [NPC.venlana]
    },
    "taleucema-arrival": {
      episode: 1,
      title: "Desembarque em Taleucema",
      summary: "A capital craterizada de Saleucami recebe a tripulação; a reunião com Venlana ocorrerá na cantina Paradise.",
      narration: "A capital craterizada de Saleucami recebe a tripulação; a reunião com Venlana ocorrerá na cantina Paradise.",
      checks: [
            {
                  skill: "Manha",
                  difficulty: "Média",
                  purpose: "avaliar os riscos de seguir os representantes do Consórcio"
            }
      ],
      choices: [
            {
                  id: "meet-at-paradise",
                  label: "Encontrar Venlana na Paradise",
                  to: "paradise-briefing"
            }
      ],
      sourceSection: "Episode I, pp. 17-19",
      locationId: LOC.saleucamiPort,
      visibleNpcIds: []
    },
    "paradise-briefing": {
      episode: 1,
      title: "Reunião na Paradise",
      summary: "Venlana apresenta o alvo, os ataques contra o Consórcio e as primeiras pistas de investigação em Saleucami.",
      narration: "Venlana apresenta o alvo, os ataques contra o Consórcio e as primeiras pistas de investigação em Saleucami.",
      checks: [
            {
                  skill: "Percepção",
                  difficulty: "Média",
                  purpose: "avaliar a postura e os interesses da contratante"
            }
      ],
      choices: [
            {
                  id: "follow-saleucami-leads",
                  label: "Investigar as pistas oferecidas",
                  to: "saleucami-leads"
            }
      ],
      sourceSection: "Episode I, pp. 18-20",
      locationId: LOC.paradise,
      visibleNpcIds: [NPC.venlana]
    },
    "saleucami-leads": {
      episode: 1,
      title: "Três pistas em Saleucami",
      summary: "As informações apontam para um ex-pirata, um receptador de carga e uma ligação com os arredores de Blackwind Crater.",
      narration: "As informações apontam para um ex-pirata, um receptador de carga e uma ligação com os arredores de Blackwind Crater.",
      checks: [
            {
                  skill: "Manha",
                  difficulty: "Média",
                  purpose: "identificar intermediários e rotas de mercadorias desviadas"
            }
      ],
      choices: [
            {
                  id: "question-graf",
                  label: "Procurar o ex-pirata Graf",
                  to: "graf-exile"
            },
            {
                  id: "question-krezo",
                  label: "Procurar o receptador Krezo",
                  to: "krezo-fence"
            },
            {
                  id: "investigate-sororinet",
                  label: "Investigar a rede SororiNet",
                  to: "sororinet-entry"
            }
      ],
      sourceSection: "Episode I, pp. 20-28",
      locationId: LOC.saleucamiPort,
      visibleNpcIds: []
    },
    // SWE13 pp. 25-28: the third lead, SororiNet, was absent in P181.
    // A failed gateway never manufactures the Vault coordinates.
    "sororinet-entry": {
      episode: 1, title: 'Primeiro acesso à SororiNet', locationId: LOC.sororinet,
      summary: 'A rede pública de turismo esconde camadas de segurança incompatíveis com um serviço comum.',
      narration: 'Os terminais apresentam rotas de excursão, mas os pedidos de acesso revelam um gateway protegido.',
      visibleNpcIds: [], sourceSection: 'Episode I, SororiNet, pp. 25-27',
      checks: [{skill:'Computadores',difficulty:'Média',purpose:'localizar e autenticar a entrada do nó secundário'}],
      choices: [
        {id:'probe-sororinet',label:'Acessar o nó secundário',to:'sororinet-secondary',condition:'check-success:sororinet-entry'},
        {id:'withdraw-sororinet',label:'Desconectar e seguir outras pistas',to:'saleucami-leads'}
      ]
    },
    "sororinet-secondary": {
      episode: 1, title: 'Segurança da SororiNet', locationId: LOC.sororinet,
      summary: 'O segundo nó protege uma passagem para dados internos da Irmandade.',
      narration: 'As permissões ficam mais restritas; a equipe pode tentar ultrapassar o gateway ou recuar.',
      visibleNpcIds: [], sourceSection: 'Episode I, SororiNet, pp. 26-27',
      checks: [{skill:'Computadores',difficulty:'Difícil',purpose:'superar a segurança do gateway interno'}],
      choices: [
        {id:'breach-final-gateway',label:'Avançar para o nó restrito',to:'sororinet-final',condition:'check-success:sororinet-secondary'},
        {id:'disconnect-secondary',label:'Interromper a intrusão',to:'saleucami-leads'}
      ]
    },
    "sororinet-final": {
      episode: 1, title: 'Dados protegidos e rastreamento', locationId: LOC.sororinet,
      summary: 'Os arquivos restritos contêm as coordenadas do Vault; Lathe pode rastrear intrusões malsucedidas.',
      narration: 'A camada final guarda os dados que ligam os ataques à cratera Blackwind.',
      visibleNpcIds: [], sourceSection: 'Episode I, SororiNet / Alerting Lathe, pp. 27-28',
      checks: [{skill:'Computadores',difficulty:'Média',purpose:'extrair as coordenadas da base antes que o acesso seja encerrado'}],
      choices: [
        {id:'extract-blackwind',label:'Guardar as coordenadas e seguir para Blackwind',to:'scrap-zone-investigation',condition:'check-success:sororinet-final',effects:{sororinetCoordinates:true}},
        {id:'disconnect-final',label:'Encerrar a conexão e seguir as outras pistas',to:'saleucami-leads'}
      ]
    },
    "graf-exile": {
      episode: 1,
      title: "O exilado Graf",
      summary: "Graf afirma conhecer a Irmandade, mas sua intenção de voltar às fileiras piratas exige cautela.",
      narration: "Graf afirma conhecer a Irmandade, mas sua intenção de voltar às fileiras piratas exige cautela.",
      checks: [
            {
                  skill: "Disciplina",
                  difficulty: "Difícil",
                  purpose: "avaliar a sinceridade do ex-pirata"
            }
      ],
      choices: [
            {
                  id: "visit-krezo-after-graf",
                  label: "Seguir a pista de mercadorias",
                  to: "krezo-fence",
                  effects: {
                        grafInterviewed: true
                  }
            }
      ],
      sourceSection: "Episode I, pp. 20-24",
      locationId: LOC.saleucamiPort,
      visibleNpcIds: [NPC.graf]
    },
    "krezo-fence": {
      episode: 1,
      title: "O receptador Krezo Wasanti",
      summary: "A loja de Krezo guarda mercadorias de procedência suspeita; uma compradora chamada Mandi liga os negócios à região de Blackwind Crater.",
      narration: "A loja de Krezo guarda mercadorias de procedência suspeita; uma compradora chamada Mandi liga os negócios à região de Blackwind Crater.",
      checks: [
            {
                  skill: "Coerção",
                  difficulty: "Média",
                  purpose: "obter informação sobre a remessa e a intermediária"
            },
            {
                  skill: "Percepção",
                  difficulty: "Difícil",
                  purpose: "localizar bens ocultos na loja"
            }
      ],
      choices: [
            {
                  id: "trace-blackwind",
                  label: "Seguir as pistas até Blackwind Crater",
                  to: "scrap-zone-investigation",
                  effects: {
                        blackwindLead: true
                  }
            }
      ],
      sourceSection: "Episode I, pp. 24-28",
      locationId: LOC.krezoShop,
      visibleNpcIds: [NPC.krezo]
    },
    "scrap-zone-investigation": {
      episode: 1,
      title: "Blackwind Crater",
      summary: "O grupo atravessa o terreno acidentado de Saleucami em direção à instalação secreta conhecida como Vault.",
      narration: "O grupo atravessa o terreno acidentado de Saleucami em direção à instalação secreta conhecida como Vault.",
      checks: [
            {
                  skill: "Sobrevivência",
                  difficulty: "Difícil",
                  purpose: "alcançar o acesso sem alertar patrulhas"
            }
      ],
      choices: [
            {
                  id: "enter-the-vault",
                  label: "Preparar a incursão ao Vault",
                  to: "veiled-stronghold"
            }
      ],
      sourceSection: "Episode I, pp. 28-31",
      locationId: LOC.blackwind,
      visibleNpcIds: []
    },
    "veiled-stronghold": {
      episode: 1,
      title: "A incursão ao Vault",
      summary: "A base da Irmandade exige planejamento, infiltração e superação de guardas antes do encontro com a figura mascarada.",
      narration: "A base da Irmandade exige planejamento, infiltração e superação de guardas antes do encontro com a figura mascarada.",
      checks: [
            {
                  skill: "Furtividade",
                  difficulty: "Difícil",
                  purpose: "atravessar a segurança do complexo"
            }
      ],
      choices: [
            {
                  id: "confront-decoy",
                  label: "Chegar até a figura mascarada",
                  to: "revah-decoy"
            }
      ],
      sourceSection: "Episode I, pp. 31-36",
      locationId: LOC.vault,
      visibleNpcIds: [NPC.revah]
    },
    "revah-decoy": {
      episode: 1,
      title: "Revah, a Rainha aparente",
      summary: "O alvo encontrado é Revah, uma impostora usada como Rainha Pirata em Saleucami; sua captura não encerra a ameaça.",
      narration: "O alvo encontrado é Revah, uma impostora usada como Rainha Pirata em Saleucami; sua captura não encerra a ameaça.",
      checks: [
            {
                  skill: "Percepção",
                  difficulty: "Média",
                  purpose: "observar indícios de identidade falsa"
            }
      ],
      choices: [
            {
                  id: "deliver-revah",
                  label: "Levar o resultado da incursão ao Consórcio",
                  to: "venlana-return",
                  effects: {
                        revahLocated: true
                  }
            }
      ],
      sourceSection: "Episode I, pp. 35-37",
      locationId: LOC.vault,
      visibleNpcIds: [NPC.revah]
    },
    "venlana-return": {
      episode: 1,
      title: "Recompensa prematura",
      summary: "O retorno ao Consórcio revela que os ataques continuam: a verdadeira Rainha opera a partir de Ord Mantell.",
      narration: "O retorno ao Consórcio revela que os ataques continuam: a verdadeira Rainha opera a partir de Ord Mantell.",
      checks: [
            {
                  skill: "Computadores",
                  difficulty: "Média",
                  purpose: "analisar a origem da nova transmissão"
            }
      ],
      choices: [
            {
                  id: "travel-ord-mantell",
                  label: "Seguir o rastro até Ord Mantell",
                  to: "ord-mantell-arrival",
                  effects: {
                        decoyExposed: true
                  }
            }
      ],
      sourceSection: "Episode I conclusion / Episode II opening, pp. 37-40",
      locationId: LOC.paradise,
      visibleNpcIds: [NPC.venlana]
    },
    "ord-mantell-arrival": {
      episode: 2,
      title: "Chegada a Worlport, Ord Mantell",
      summary: "O rastro da transmissão leva a Worlport, onde o contato Jander e o circuito de apostas de Illo Vandin oferecem novas pistas.",
      narration: "O rastro da transmissão leva a Worlport, onde o contato Jander e o circuito de apostas de Illo Vandin oferecem novas pistas.",
      checks: [
            {
                  skill: "Manha",
                  difficulty: "Média",
                  purpose: "localizar o contato do Consórcio sem revelar a investigação"
            }
      ],
      choices: [
            {
                  id: "meet-jander",
                  label: "Encontrar Jander em Worlport",
                  to: "jander-contact"
            }
      ],
      sourceSection: "Episode II, pp. 38-49",
      locationId: LOC.worlport,
      visibleNpcIds: [NPC.jander]
    },
    "jander-contact": {
      episode: 2,
      title: "O contato Jander",
      summary: "Jander orienta a tripulação a procurar Illo Vandin, um poderoso apostador e criminoso local.",
      narration: "Jander orienta a tripulação a procurar Illo Vandin, um poderoso apostador e criminoso local.",
      checks: [
            {
                  skill: "Negociação",
                  difficulty: "Média",
                  purpose: "obter uma apresentação ao círculo de Vandin"
            }
      ],
      choices: [
            {
                  id: "visit-vandin",
                  label: "Procurar Illo Vandin",
                  to: "vandin-casino"
            }
      ],
      sourceSection: "Episode II, pp. 48-52",
      locationId: LOC.worlport,
      visibleNpcIds: [NPC.jander]
    },
    "vandin-casino": {
      episode: 2,
      title: "A barganha de Illo Vandin",
      summary: "Illo Vandin aceita fornecer uma pista decisiva somente após o grupo cumprir um dos dois serviços que exige.",
      narration: "Illo Vandin aceita fornecer uma pista decisiva somente após o grupo cumprir um dos dois serviços que exige.",
      checks: [
            {
                  skill: "Negociação",
                  difficulty: "Difícil",
                  purpose: "negociar os termos de um favor"
            }
      ],
      choices: [
            {
                  id: "fix-the-fight",
                  label: "Investigar o esquema de lutas",
                  to: "fight-fixing"
            },
            {
                  id: "reach-rusty",
                  label: "Seguir a missão de Rusty Reach",
                  to: "rusty-reach"
            }
      ],
      sourceSection: "Episode II, pp. 50-59",
      locationId: LOC.vandinCasino,
      visibleNpcIds: [NPC.illo]
    },
    "fight-fixing": {
      episode: 2,
      title: "Manipulação das lutas",
      summary: "A primeira opção de Vandin envolve um circuito de lutas e apostas; a decisão do grupo deixa uma dívida com o criminoso.",
      narration: "A primeira opção de Vandin envolve um circuito de lutas e apostas; a decisão do grupo deixa uma dívida com o criminoso.",
      checks: [
            {
                  skill: "Enganação",
                  difficulty: "Difícil",
                  purpose: "lidar com os interesses de apostadores e lutadores"
            }
      ],
      choices: [
            {
                  id: "collect-vandin-favor-fight",
                  label: "Cobrar a informação prometida",
                  to: "consort-lead",
                  effects: {
                        vandinFavor: "fight-fixing"
                  }
            }
      ],
      sourceSection: "Episode II, pp. 53-58",
      locationId: LOC.vandinCasino,
      visibleNpcIds: [NPC.illo]
    },
    "rusty-reach": {
      episode: 2,
      title: "Rusty Reach",
      summary: "A segunda opção leva a um assentamento isolado nas terras de sucata, com riscos ambientais e guardas.",
      narration: "A segunda opção leva a um assentamento isolado nas terras de sucata, com riscos ambientais e guardas.",
      checks: [
            {
                  skill: "Sobrevivência",
                  difficulty: "Difícil",
                  purpose: "chegar ao local com recursos suficientes"
            }
      ],
      choices: [
            {
                  id: "collect-vandin-favor-reach",
                  label: "Voltar com o favor concluído",
                  to: "consort-lead",
                  effects: {
                        vandinFavor: "rusty-reach"
                  }
            }
      ],
      sourceSection: "Episode II, pp. 59-64",
      locationId: LOC.rustyReach,
      visibleNpcIds: [NPC.illo]
    },
    "consort-lead": {
      episode: 2,
      title: "A pista do consorte",
      summary: "A informação de Vandin permite localizar um aliado íntimo da Rainha e demonstrar que ela o está enganando.",
      narration: "A informação de Vandin permite localizar um aliado íntimo da Rainha e demonstrar que ela o está enganando.",
      checks: [
            {
                  skill: "Encanto",
                  difficulty: "Difícil",
                  purpose: "convencer o consorte a revelar o acesso ao esconderijo"
            }
      ],
      choices: [
            {
                  id: "enter-queen-palace",
                  label: "Localizar o palácio subterrâneo",
                  to: "palace-infiltration"
            }
      ],
      sourceSection: "Episode II, pp. 64-67",
      locationId: LOC.worlport,
      visibleNpcIds: []
    },
    "palace-infiltration": {
      episode: 2,
      title: "Palácio subterrâneo de Worlport",
      summary: "A equipe invade ou infiltra o palácio secreto da Irmandade, protegido por seus seguidores e por Ryale Wei.",
      narration: "A equipe invade ou infiltra o palácio secreto da Irmandade, protegido por seus seguidores e por Ryale Wei.",
      checks: [
            {
                  skill: "Furtividade",
                  difficulty: "Difícil",
                  purpose: "alcançar o centro do palácio sem ser cercado"
            }
      ],
      choices: [
            {
                  id: "face-ryale",
                  label: "Confrontar Ryale Wei",
                  to: "ryale-revelation"
            }
      ],
      sourceSection: "Episode II, pp. 66-71",
      locationId: LOC.queenPalace,
      visibleNpcIds: [NPC.ryale]
    },
    "ryale-revelation": {
      episode: 2,
      title: "O segredo da máscara",
      summary: "Ryale Wei revela a natureza sucessória do título de Rainha Pirata e o paradeiro da nave Renegade’s Blood.",
      narration: "Ryale Wei revela a natureza sucessória do título de Rainha Pirata e o paradeiro da nave Renegade’s Blood.",
      checks: [
            {
                  skill: "Coerção",
                  difficulty: "Difícil",
                  purpose: "obter informações sobre a frota sem destruir possíveis alianças"
            }
      ],
      choices: [
            {
                  id: "pursue-renegades-blood",
                  label: "Preparar interceptação da nau-capitânia",
                  to: "fleet-intercept",
                  effects: {
                        trueQueenIdentified: true
                  }
            }
      ],
      sourceSection: "Episode II, pp. 66-71",
      locationId: LOC.queenPalace,
      visibleNpcIds: [NPC.ryale]
    },
    "fleet-intercept": {
      episode: 3,
      title: "Preparar a interceptação",
      summary: "O grupo planeja como localizar a Renegade’s Blood em meio a uma batalha com as forças do Consórcio Zann.",
      narration: "O grupo planeja como localizar a Renegade’s Blood em meio a uma batalha com as forças do Consórcio Zann.",
      checks: [
            {
                  skill: "Astrogação",
                  difficulty: "Difícil",
                  purpose: "traçar uma aproximação à nave pirata"
            }
      ],
      choices: [
            {
                  id: "approach-flagship",
                  label: "Aproximar-se da nau-capitânia",
                  to: "flagship-systems"
            }
      ],
      sourceSection: "Episode III, pp. 72-75",
      locationId: LOC.spaceBattle,
      visibleNpcIds: [NPC.sykes]
    },
    "flagship-systems": {
      episode: 3,
      title: "Defesas e comunicações",
      summary: "Desabilitar defesas e comunicações da nau pode tornar o embarque menos arriscado; a batalha continua ao redor.",
      narration: "Desabilitar defesas e comunicações da nau pode tornar o embarque menos arriscado; a batalha continua ao redor.",
      checks: [
            {
                  skill: "Artilharia",
                  difficulty: "Difícil",
                  purpose: "atingir subsistemas em vez de destruir o casco"
            }
      ],
      choices: [
            {
                  id: "board-the-ship",
                  label: "Executar abordagem da nave",
                  to: "renegades-blood-boarding"
            }
      ],
      sourceSection: "Episode III, pp. 75-84",
      locationId: LOC.spaceBattle,
      visibleNpcIds: []
    },
    "renegades-blood-boarding": {
      episode: 3,
      title: "Abordagem à Renegade’s Blood",
      summary: "A tripulação atravessa corredores avariados e a resistência da Irmandade para chegar à ponte.",
      narration: "A tripulação atravessa corredores avariados e a resistência da Irmandade para chegar à ponte.",
      checks: [
            {
                  skill: "Coordenação",
                  difficulty: "Difícil",
                  purpose: "superar os perigos da nave danificada"
            }
      ],
      choices: [
            {
                  id: "reach-queen-bridge",
                  label: "Alcançar a ponte de comando",
                  to: "flagship-showdown"
            }
      ],
      sourceSection: "Episode III, pp. 84-90",
      locationId: LOC.shipCorridors,
      visibleNpcIds: []
    },
    "flagship-showdown": {
      episode: 3,
      title: "Noira, a verdadeira Rainha Pirata",
      summary: "A Rainha Noira oferece uma alternativa à captura: aliança com a Irmandade ou confronto pelo controle do navio.",
      narration: "A Rainha Noira oferece uma alternativa à captura: aliança com a Irmandade ou confronto pelo controle do navio.",
      checks: [
            {
                  skill: "Frieza",
                  difficulty: "Difícil",
                  purpose: "resistir à pressão e avaliar a proposta da Rainha"
            }
      ],
      choices: [
            {
                  id: "capture-noira",
                  label: "Concluir a caçada pelo contrato",
                  to: "paradise-final-handover",
                  condition: "encounter-resolved:pirate-queen-duel",
                  effects: {
                        queenCaptured: true,
                        resolutionMode: "capture"
                  }
            },
            {
                  id: "join-sorority",
                  label: "Aceitar uma aliança com a Irmandade",
                  to: "mpq-epilogue",
                  condition: "encounter-resolved:pirate-queen-duel",
                  effects: {
                        queenAlliance: true,
                        resolutionMode: "alliance"
                  }
            }
      ],
      sourceSection: "Episode III, pp. 90-92",
      locationId: LOC.flagshipBridge,
      visibleNpcIds: [NPC.noira]
    },
    // SWE13 pp. 93-95: Porel exposes Venlana as the Sorority mole.
    // The final Consortium payment must not be issued before this choice.
    "paradise-final-handover": {
      episode: 3, title: 'A disputa pela máscara na Paradise', locationId: LOC.paradise,
      summary: 'Porel Vakra e Venlana Sipal acusam um ao outro de traição; a escolha altera a recompensa e a perseguição futura.',
      narration: 'O retorno à Paradise revela duas versões incompatíveis sobre a lealdade de Venlana. A tripulação decide a quem entregar a máscara.',
      visibleNpcIds: [NPC.porel,NPC.venlana], sourceSection: 'Episode III, Showdown at the Paradise, pp. 93-95',
      checks: [{skill:'Disciplina',difficulty:'Difícil',purpose:'perceber a falsidade da acusação feita por Venlana'}],
      choices: [
        {id:'side-porel',label:'Apoiar Porel e entregar Venlana ao Consórcio',to:'mpq-epilogue',effects:{resolutionMode:'porel',venlanaExposed:true,porelLoyal:true}},
        {id:'side-venlana',label:'Apoiar Venlana contra Porel',to:'mpq-epilogue',effects:{resolutionMode:'venlana',porelBetrayed:true,consortiumRetaliation:true}},
        {id:'reject-both',label:'Recusar ambos e abandonar a recompensa',to:'mpq-epilogue',effects:{resolutionMode:'independent',consortiumRetaliation:true}}
      ]
    },
    "mpq-epilogue": {
      episode: 3,
      title: "O destino da máscara",
      summary: "O resultado do confronto define a relação futura do grupo com o Consórcio Zann e com a Irmandade Oculta.",
      narration: "O resultado do confronto define a relação futura do grupo com o Consórcio Zann e com a Irmandade Oculta.",
      checks: [],
      choices: [],
      sourceSection: "Episode III conclusion, pp. 92-93",
      locationId: LOC.worlport,
      visibleNpcIds: []
    }
  }
});


// Source-anchored locations and persistent NPCs. Existing user edits always win.
function seedMaskWorld(world) {
  const next = clone(world || {});
  next.locations = { ...(next.locations || {}) };
  next.npcs = { ...(next.npcs || {}) };
  next.factions = { ...(next.factions || {}) };
  const places = [
    [LOC.saleucamiPort,'Taleucema, Saleucami','Capital construída dentro de uma cratera; encontro inicial com o Consórcio.'],
    [LOC.paradise,'Cantina Paradise','Local da reunião com Venlana, próximo ao porto de Taleucema.'],
    [LOC.krezoShop,'Krezo’s Imports','Loja e ponto de revenda de mercadorias em Saleucami.'],
    [LOC.blackwind,'Blackwind Crater','Cratera remota com acesso ao esconderijo da Irmandade.'],
    [LOC.vault,'The Vault','Instalação fortificada da Irmandade em Blackwind Crater.'],
    [LOC.worlport,'Worlport, Ord Mantell','Capital de Ord Mantell, sede de cassinos e intermediários.'],
    [LOC.vandinCasino,'Círculo de Illo Vandin','Rede de jogos e apostas usada por Illo Vandin.'],
    [LOC.rustyReach,'Rusty Reach','Assentamento nas terras de sucata de Ord Mantell.'],
    [LOC.queenPalace,'Palácio subterrâneo','Esconderijo da Irmandade sob Worlport.'],
    [LOC.spaceBattle,'Batalha no espaço','Região da interceptação à Renegade’s Blood.'],
    [LOC.shipCorridors,'Corredores da Renegade’s Blood','Passagens avariadas e defendidas por corsários.'],
    [LOC.flagshipBridge,'Ponte da Renegade’s Blood','Centro de comando da verdadeira Rainha Pirata.'],
    [LOC.sororinet,'Nó SororiNet','Acesso público que esconde dados de navegação da Irmandade.']
  ];
  for (const [id,name,description] of places) next.locations[id] ??= loc(id,name,description);
  next.factions[FACTION.veiledSorority] ??= {id:FACTION.veiledSorority,name:'Veiled Sorority',objective:'Preservar a frota e a identidade da Rainha.',status:'active',knownToPlayer:false,tags:['published-adventure',ID]};
  next.factions[FACTION.zannConsortium] ??= {id:FACTION.zannConsortium,name:'Zann Consortium',objective:'Encerrar os ataques aos seus carregamentos.',status:'active',knownToPlayer:true,tags:['published-adventure',ID]};
  const people = [
    [NPC.venlana,'Venlana Sipal',LOC.paradise,FACTION.zannConsortium,{species:'Pantorana',occupation:'Subchefe do Consórcio',knownToPlayer:true,goal:'Conduzir a contratação sem expor os próprios interesses.'}],
    [NPC.graf,'Graf Lind',LOC.saleucamiPort,FACTION.veiledSorority,{occupation:'Ex-pirata',goal:'Voltar a ter influência entre os corsários.'}],
    [NPC.krezo,'Krezo Wasanti',LOC.krezoShop,null,{species:'Rodiano',occupation:'Receptador',goal:'Evitar represálias pelo comércio de cargas roubadas.'}],
    [NPC.revah,'Revah',LOC.vault,FACTION.veiledSorority,{species:'Humana',occupation:'Isca mascarada',goal:'Proteger a identidade da verdadeira Rainha.'}],
    [NPC.jander,'Jander',LOC.worlport,FACTION.zannConsortium,{species:'Toydariano',occupation:'Contato local do Consórcio',goal:'Orientar a investigação sem se expor.'}],
    [NPC.illo,'Illo Vandin',LOC.vandinCasino,null,{species:'Herglic',occupation:'Magnata do jogo',goal:'Cobrar favores antes de compartilhar informações.'}],
    [NPC.ryale,'Ryale Wei',LOC.queenPalace,FACTION.veiledSorority,{species:'Humana',occupation:'Oficial da Irmandade',goal:'Defender a liderança da Rainha.'}],
    [NPC.noira,'Noira',LOC.flagshipBridge,FACTION.veiledSorority,{species:'Ubese',occupation:'Verdadeira Rainha Pirata',goal:'Preservar a autonomia da frota.'}],
    [NPC.sykes,'Jerid Sykes',LOC.spaceBattle,FACTION.zannConsortium,{species:'Humano',occupation:'Comandante da frota do Consórcio',goal:'Proteger os comboios de Tyber Zann.'}],
    [NPC.porel,'Porel Vakra',LOC.paradise,FACTION.zannConsortium,{species:'Twi’lek',occupation:'Subchefe do Consórcio',goal:'Investigar a lealdade de Venlana sem revelar sua missão.'}],
    [NPC.lathe,'Lathe',LOC.sororinet,FACTION.veiledSorority,{species:'Humana',occupation:'Slicer da Irmandade',goal:'Rastrear intrusos na SororiNet.'}]
  ];
  for (const [id,name,locationId,factionId,profile] of people) next.npcs[id] ??= npc(id,name,locationId,factionId,profile);
  try { ensureLivingWorldDepth(next); } catch { /* engine without living-depth service remains playable */ }
  return next;
}

// P180 saves retain history, choices, checks and event IDs. Only a v1
// introductory Ord Mantell scene is re-homed to its actual Saleucami start.
// Both legacy progress and UAE snapshot must agree before reconciliation.
export function migrateMaskOfThePirateQueenWorld(world) {
  if (world?.publishedAdventure?.id !== ID || Number(world.publishedAdventure.schemaVersion || 1) >= 2) return world;
  const next = clone(world);
  const p = next.publishedAdventure;
  const previousNode = p.currentNodeId;
  const remap = id => id === 'ord-mantell-arrival' ? 'saleucami-contract-offer' : id;
  p.currentNodeId = remap(p.currentNodeId);
  p.visitedNodeIds = [...new Set((p.visitedNodeIds || []).map(remap))];
  if (!p.visitedNodeIds.includes(p.currentNodeId)) p.visitedNodeIds.push(p.currentNodeId);
  p.schemaVersion = 2;
  p.migration = {from:1,to:2,previousNodeId:previousNode,kind:'source-correction-saleucami',migratedAt:now()};
  const r = next.publishedAdventureRuntime;
  if (r?.adventureId === ID) {
    r.currentNodeId = remap(r.currentNodeId);
    r.visitedNodeIds = [...new Set((r.visitedNodeIds || []).map(remap))];
    r.currentLocationId = MASK_OF_THE_PIRATE_QUEEN.nodes[r.currentNodeId]?.locationId ?? r.currentLocationId;
    // Canonical history is immutable; old events still refer to their original source node.
  }
  return next;
}

function conditionOk(choice, progress) {
  const c = choice?.condition;
  if (!c) return true;
  if (typeof c !== 'string') return false;
  if (c.startsWith('unvisited:')) return !progress.visitedNodeIds.includes(c.slice(10));
  if (c.startsWith('visited:')) return progress.visitedNodeIds.includes(c.slice(8));
  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);
  if (c.startsWith('check-success:')) return (progress.checkHistory || []).some(x => x.nodeId === c.slice(14) && x.success === true);
  if (c.startsWith('check-failure:')) return (progress.checkHistory || []).some(x => x.nodeId === c.slice(14) && x.success === false);
  if (c.startsWith('encounter-resolved:')) return (progress.encounterHistory || []).some(x =>
    x.encounterId === c.slice(19) && ['adversaries_defeated','negotiated'].includes(x.outcome));
  return false; // Unknown conditions must never unlock a scene or reward.

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
    title: MASK_OF_THE_PIRATE_QUEEN.title,
    status: 'ACTIVE',
    currentNodeId: MASK_OF_THE_PIRATE_QUEEN.startNodeId,
    visitedNodeIds: [MASK_OF_THE_PIRATE_QUEEN.startNodeId],
    flags: {},
    clocks: clone(MASK_OF_THE_PIRATE_QUEEN.initialClocks),
    checkHistory: [],
    encounterHistory: [],
    schemaVersion: 3,
    consequencesApplied: false,
    startedAt: now(),
    updatedAt: now(),
    completedAt: null,
    rewardsApplied: Boolean(rewardsApplied),
    source: SOURCE,
    implementation: clone(MASK_OF_THE_PIRATE_QUEEN.implementation)
  };
}

export function startMaskOfThePirateQueen(world) {
  const next = seedMaskWorld(world);
  next.publishedAdventure = baseProgress();
  const p = next.publishedAdventure;
  return {
    world: next,
    progress: clone(p),
    node: playerNode(MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId], p)
  };
}

export function advanceMaskOfThePirateQueen(world, choiceId) {
  const next = seedMaskWorld(migrateMaskOfThePirateQueenWorld(world));
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');
  if (p.activeEncounterId) throw new Error('Encerre o encontro ativo antes de avançar de cena.');
  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];
  const choice = (node?.choices || []).find(c => c.id === choiceId);
  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha inválida.');
  p.flags = { ...(p.flags || {}), ...(choice.effects || {}) };
  p.currentNodeId = choice.to;
  if (!p.visitedNodeIds.includes(choice.to)) p.visitedNodeIds.push(choice.to);
  const target = MASK_OF_THE_PIRATE_QUEEN.nodes[choice.to];
  if (!target.choices || target.choices.length === 0) {
    p.status = 'COMPLETED';
    p.completedAt = now();
    // Store post-adventure consequences as data, never inventing a paid reward.
    const mode = p.flags?.resolutionMode || 'unknown';
    next.publishedAdventureConsequences = {...(next.publishedAdventureConsequences||{}),[ID]:{
      resolutionMode:mode,venlanaExposed:Boolean(p.flags?.venlanaExposed),
      consortiumRetaliation:Boolean(p.flags?.consortiumRetaliation || mode === 'alliance'),
      noiraCaptured:Boolean(p.flags?.queenCaptured),recordedAt:p.completedAt,
      source:'SWE13 Episode III conclusion',editoriallyCertified:false
    }};
    if (mode === 'porel' && next.npcs?.[NPC.venlana]) next.npcs[NPC.venlana].status='captured';
    if (mode === 'venlana' && next.npcs?.[NPC.porel]) next.npcs[NPC.porel].status='fugitive';
    if (mode === 'alliance' && next.npcs?.[NPC.noira]) next.npcs[NPC.noira].relation='ally';
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

export function maskOfThePirateQueenStatus(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) {
    return {
      available: true,
      active: false,
      catalog: { id: ID, title: MASK_OF_THE_PIRATE_QUEEN.title, campaignModes: [...MASK_OF_THE_PIRATE_QUEEN.campaignModes], rulesProfile: MASK_OF_THE_PIRATE_QUEEN.rulesProfile }
    };
  }
  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];
  return {
    available: true,
    active: p.status === 'ACTIVE',
    completed: p.status === 'COMPLETED',
    archived: p.status === 'ARCHIVED',
    progress: clone(p),
    node: playerNode(node, p)
  };
}

export function currentMaskOfThePirateQueenScene(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) return null;
  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];
  if (!node) return null;
  return { locationId: node.locationId, title: node.title, narration: node.narration, visibleNpcIds: [...(node.visibleNpcIds || [])] };
}

export function currentMaskOfThePirateQueenChecks(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') return [];
  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];
  return clone(node?.checks || []);
}

export function adjudicateMaskOfThePirateQueenCheck(world, { checkIndex = 0, result = {} } = {}) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');
  const checks = currentMaskOfThePirateQueenChecks(next);
  const check = checks[checkIndex];
  if (!check) throw new Error('Teste inválido.');
  if (typeof result.success !== 'boolean') throw new Error('Resultado de teste deve informar sucesso ou falha.');
  p.checkHistory = Array.isArray(p.checkHistory) ? p.checkHistory : [];
  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, skill: check.skill, success: result.success,
    netAdvantage: Number.isFinite(Number(result.netAdvantage)) ? Number(result.netAdvantage) : 0 };
  p.checkHistory.push(entry);
  p.flags = { ...(p.flags || {}), [`check:${p.currentNodeId}:${checkIndex}`]: result.success };
  if (!result.success) {
    const alert = p.clocks?.pirateAlert;
    if (alert && ['scrap-zone-investigation','veiled-stronghold','palace-infiltration','renegades-blood-boarding','sororinet-entry','sororinet-secondary','sororinet-final'].includes(p.currentNodeId)) {
      alert.value = Math.min(alert.max, (Number(alert.value) || 0) + 1);
      alert.status = alert.value >= alert.max ? 'COMPLETED' : 'ACTIVE';
      if (p.currentNodeId.startsWith('sororinet-')) p.flags.sororinetIntrusionDetected = true;
    }
  }
  p.updatedAt = now();
  next.publishedAdventure = p;
  return { world: next, progress: clone(p), adjudication: entry };
}

export function currentMaskOfThePirateQueenMasterRequest(world) {
  const s = maskOfThePirateQueenStatus(world);
  if (!s.active && !s.completed) return null;
  const node = s.node;
  if (!node) return null;
  return {
    kind: 'published_adventure',
    adventureId: ID,
    adventureTitle: MASK_OF_THE_PIRATE_QUEEN.title,
    nodeId: node.id,
    episode: node.episode,
    locationId: node.locationId,
    summary: node.summary,
    checks: currentMaskOfThePirateQueenChecks(world),
    suggestedActions: (node.choices || []).map(c => c.label),
    antiSpoiler: true,
    source: SOURCE
  };
}

export function currentMaskOfThePirateQueenEncounter(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') return null;
  const scenes = {
    'veiled-stronghold':['vault-sorority-guards','Guardas do Vault'],
    'palace-infiltration':['worlport-sorority-guards','Defensores do palácio'],
    'renegades-blood-boarding':['renegades-blood-corsairs','Corsários a bordo'],
    'flagship-showdown':['pirate-queen-duel','Confronto com Noira'],
    'paradise-final-handover':['porel-venlana-standoff','Confronto na Paradise']
  };
  const pair = scenes[p.currentNodeId];
  if (!pair) return null;
  const [id,label] = pair;
  const resolved = (p.encounterHistory || []).some(x => x.encounterId === id && x.outcome);
  return {id,label,available:!resolved,status:resolved?'resolved':p.activeEncounterId===id?'active':'ready'};
}

export function maskOfThePirateQueenEncounterSpec(world, encounterId = null) {
  const current = currentMaskOfThePirateQueenEncounter(world);
  if (!current || !current.available || (encounterId && encounterId !== current.id))
    throw new Error('Encontro publicado indisponível na cena atual.');
  const final = current.id === 'pirate-queen-duel';
  const paradise = current.id === 'porel-venlana-standoff';
  return {id:current.id,label:current.label,playerInitiativeSkill:'frialdad',npcInitiativeSkill:'alerta',
    // SWE13 p. 90: two minion groups of three bridge guards, plus Noira.
    // Porel is a source-checked nemesis; confrontation remains optional.
    adversaries:final?[
      {profileKey:'mpq_bridge_guard',count:3,range:'Curto'},
      {profileKey:'mpq_bridge_guard',count:3,range:'Curto'},
      {profileKey:'mpq_noira',worldNpcId:NPC.noira,range:'Médio'}
    ]:paradise?[{profileKey:'mpq_porel',worldNpcId:NPC.porel,range:'Curto'}]
      :[{profileKey:'veiled_sorority_corsair',count:3,range:'Curto'}]};
}

export function markMaskOfThePirateQueenEncounterActive(world, encounterId) {
  const next = clone(world || {}),p = next.publishedAdventure;
  const encounter = currentMaskOfThePirateQueenEncounter(next);
  if (!encounter || !encounter.available || encounter.id !== encounterId || p.activeEncounterId)
    throw new Error('Encontro não pode ser iniciado nesta cena.');
  p.activeEncounterId = encounterId;
  p.updatedAt = now();
  return next;
}

export function recordMaskOfThePirateQueenEncounterOutcome(world, {encounterId,outcome,adversaries=[]} = {}) {
  const next = clone(world || {}),p = next.publishedAdventure;
  const encounter = currentMaskOfThePirateQueenEncounter(next);
  if (!encounter || !encounter.available || encounter.id !== encounterId ||
      (p.activeEncounterId && p.activeEncounterId !== encounterId))
    throw new Error('Resultado de encontro incompatível com a cena.');
  if (!['adversaries_defeated','players_retreat','negotiated','ended'].includes(outcome))
    throw new Error('Resultado de encontro inválido.');
  p.encounterHistory = Array.isArray(p.encounterHistory) ? p.encounterHistory : [];
  const record = {at:now(),nodeId:p.currentNodeId,encounterId,outcome,
    adversaries:(Array.isArray(adversaries)?adversaries:[]).slice(0,24).map(a=>({id:a.id??null,profileKey:a.profileKey??null,status:a.status??null}))};
  p.encounterHistory.push(record);
  p.activeEncounterId = null;
  p.flags = {...(p.flags||{}),[`encounter:${encounterId}`]:outcome};
  if (encounterId === 'pirate-queen-duel' && outcome === 'adversaries_defeated') p.flags.noiraDefeated = true;
  p.updatedAt = now();
  return {world:next,record:clone(record)};
}

export function markMaskOfThePirateQueenRewardsApplied(world, details = {}) {
  if (world?.publishedAdventure?.id !== ID || world.publishedAdventure.status !== 'COMPLETED') throw new Error('Recompensa exige conclusão da aventura.');
  if (world?.publishedAdventureRewards?.[ID]?.applied) throw new Error('Recompensa já aplicada.');
  const next = clone(world || {});
  next.publishedAdventureRewards = { ...(next.publishedAdventureRewards || {}) };
  next.publishedAdventureRewards[ID] = {
    applied: true,
    appliedAt: now(),
    credits: Number.isFinite(Number(details.credits)) ? Math.max(0, Number(details.credits)) : 0,
    obligationReduced: Number.isFinite(Number(details.obligationReduced)) ? Math.max(0, Number(details.obligationReduced)) : 0,
    note: details.note || 'Recompensa pela captura da Rainha Pirata'
  };
  if (next?.publishedAdventure?.id === ID) {
    next.publishedAdventure.rewardsApplied = true;
    next.publishedAdventure.updatedAt = now();
  }
  return next;
}

export function archiveMaskOfThePirateQueen(world) {
  const next = clone(world || {});
  const p = next?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'COMPLETED') {
    throw new Error('A Máscara da Rainha Pirata precisa estar concluída antes de arquivar.');
  }
  p.status = 'ARCHIVED';
  p.archivedAt = now();
  p.updatedAt = now();
  next.publishedAdventure = p;
  return next;
}
