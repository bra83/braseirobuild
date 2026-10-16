import { ensureLivingWorldDepth } from '../world/livingWorldDepth.mjs';
import { FACTS,ARGUMENTS,guardCount,scoreDimmock,validateGeonosis,recordGeonosisChoice } from './swe02GeonosisP237.mjs';
import { SHIP_CHECKS,shipCheckAvailable,shipNoisePool,recordShipCheck,recordShipChoice,validateShip,shipSmellSetback } from './swe02KraytFangP238.mjs';
import {validateThwheek} from './swe02ThwheekP239.mjs';
import {validateNabat} from './swe02NabatP240.mjs';
import {validateSWE02Social,remainingDrombbThugs} from './swe02SocialP241.mjs';
import {validateNewMeenP242,validateNewMeenSafe} from './swe02NewMeenP242.mjs';
import {validateNewMeenP243} from './swe02EvacuationP243.mjs';
import {validateSWE02Act3,swe02Act3Consequences} from './swe02Act3Resolution.mjs';
import {validateSWE02Palace} from './swe02Act3PalaceWip.mjs';
import {validateSWE02Anatta} from './swe02Act2Anatta.mjs';
import {validateSWE02Act2Contracts,askSWE02Piddock} from './swe02Act2ContractsWip.mjs';
import {validateSWE02PalaceOperations} from './swe02PalaceOperations.mjs';

const ID = 'long-arm-of-the-hutt';
const SOURCE = { title: 'Long Arm of the Hutt', system: 'Star Wars: Edge of the Empire', productCode: 'SWE02', userMaterial: true };
const LOC = {
  rylosHangar: 'laoth.loc.rylos_hangar',
  geonoOrbit: 'laoth.loc.geono_orbit',
  geonoSurface: 'laoth.loc.geono_surface',
  nandina: 'laoth.loc.nandina_palace',
  teemoPalace: 'laoth.loc.teemo_palace',
  nabat: 'laoth.loc.nabat',
  lylekDen: 'laoth.loc.lylek_den',
  newMeen: 'laoth.loc.new_meen',
  piddockParty: 'laoth.loc.piddock_party'
};
const NPC = {
  teemo: 'laoth.npc.teemo',
  thwheel: 'laoth.npc.thwheel',
  dromund: 'laoth.npc.dromund',
  vorn: 'laoth.npc.vorn',
  ota: 'laoth.npc.ota',
  nyn: 'laoth.npc.nyn',
  bura: 'laoth.npc.bura_ban',
  piddock: 'laoth.npc.duke_piddock',
  dimmock: 'laoth.npc.duke_dimmock',
  thwheek: 'laoth.npc.thwheek'
};
const FACTION = {
  teemoSyndicate: 'laoth.fac.teemo',
  hutts: 'laoth.fac.hutt_cartel',
  geonosians: 'laoth.fac.geonosian_hive'
};

function clone(v) { return structuredClone(v); }
function now() { return new Date().toISOString(); }
function clamp(v, min, max) { return Math.min(max, Math.max(min, Number(v) || 0)); }
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
function faction(id, name, objective, knownToPlayer = false) {
  return { id, name, objective, status: 'active', resources: [], relations: {}, facts: [], knownToPlayer, tags: ['published-adventure', ID], plans: [], updatedAt: now() };
}

export const LONG_ARM_OF_THE_HUTT = Object.freeze({
  id: ID,
  title: 'O Longo Braço do Hutt',
  originalTitle: 'Long Arm of the Hutt',
  rulesProfile: 'EOTE_CORE',
  campaignModes: ['EOTE', 'MIXED'],
  source: SOURCE,
  startNodeId: 'rylos-escape-aftermath',
  schemaVersion: 2,
  initialClocks: {
    huttBounty: { value: 0, max: 6, status: 'ACTIVE' }
  },
  implementation: { episodesImplemented: [1, 2, 3], episodesTotal: 3, boundaryNodeId: null },
  nodes: {
    'krayt-fang-logs': {
      episode:1,title:'Registros da Krayt Fang',locationId:LOC.rylosHangar,
      summary:'Antes de pousar, a tripulação examina os registros da nave e um possível sinalizador de Teemo.',
      narration:'O ruído dos motores encobre um aviso no terminal de bordo. A rota de Ryloth ainda não está segura.',
      visibleNpcIds:[],checks:[{skill:'Computadores',difficulty:'Média',purpose:'localizar registros e sinais de rastreamento'}],
      choices:[
        {id:'finish-ship-inspection',label:'Continuar pela rota legada (inspeção NÃO certificada)',to:'ryloth-safehouse'},
        {id:'sivor-shell',label:'Guardar a carapaça do porão três (SWE02 p.7)',to:'krayt-fang-logs'},
        {id:'ship-find-bura',label:'Localizar e libertar B’ura B’an no porão três (sem teste, p.9)',to:'krayt-fang-logs',condition:'!flag:shipBuraFound'},
        {id:'ship-depart-verified',label:'Seguir para Ryloth após traçar a rota e encontrar B’ura B’an',to:'rylos-arrival-decision',condition:'flag:shipDepartureVerified'}]
    },
    'rylos-arrival-decision':{
      episode:1,title:'Saída do hiperespaço em Ryloth',locationId:LOC.rylosHangar,
      summary:'A rota e o rastreador determinam se Thwheek consegue interceptar a nave.',
      narration:'As estrelas retornam aos pontos luminosos. Ryloth cresce sob o casco da Krayt Fang.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'ship-follow-interception',label:'Enfrentar o caça que se aproxima',to:'thwheek-orbit',condition:'flag:shipThwheekInterceptionRequired'},
        {id:'ship-safe-landing',label:'Pousar em Nabat sem interceptação',to:'nabat-arrival',condition:'flag:shipThwheekSkipped'}]
    },
    'thwheek-orbit':{
      episode:1,title:'Interceptação em órbita de Ryloth',locationId:LOC.rylosHangar,
      summary:'SWE02 p.12: Thwheek ataca a Krayt Fang a alcance Curto; combate veicular obrigatório.',
      narration:'Um caça Dunelizard surge da dobra hiperespacial e se alinha para atacar o cargueiro.',
      visibleNpcIds:[NPC.thwheek],checks:[],choices:[
        {id:'ship-start-interception',label:'Iniciar combate veicular real contra Thwheek',to:'thwheek-engagement',condition:'flag:shipThwheekInterceptionRequired'}]
    },
    'thwheek-engagement':{
      episode:1,title:'Duelo espacial com Thwheek',locationId:LOC.rylosHangar,
      summary:'O motor veicular aplica dados, dano de casco e manobras. Thwheek foge após 5 traumas; pode ser perseguido.',
      narration:'O Dunelizard acelera sobre a proa; cada rajada arranca fagulhas dos escudos.',
      visibleNpcIds:[NPC.thwheek],checks:[],choices:[
        {id:'ship-thwheek-to-nabat',label:'Prosseguir a Nabat após confronto resolvido',to:'nabat-arrival',condition:'flag:shipThwheekLandedSafely'},
        {id:'ship-thwheek-crash',label:'Realizar pouso de emergência após queda da Krayt Fang',to:'thwheek-crash',condition:'flag:shipCrashCrewPending'}]
    },
    'thwheek-crash':{
      episode:1,title:'Queda e pouso de emergência em Ryloth',locationId:LOC.nabat,
      summary:'SWE02 p.12: Pilotagem Difícil, depois Aguante Difícil para cada tripulante; falhas geram Crítico e Ameaças ferimentos.',
      narration:'O casco treme em queda livre. O solo de Ryloth cresce na janela enquanto a tripulação se agarra aos assentos.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'ship-crash-to-nabat',label:'Seguir para Nabat após resolver TODA a tripulação',to:'nabat-arrival',condition:'flag:shipCrashCrewResolved'}]
    },
    'nabat-arrival':{
      episode:1,title:'Chegada ao porto de Nabat',locationId:LOC.nabat,
      summary:'SWE02 p.13: B’ura B’an providencia abrigo, reparos e reabastecimento por contatos locais.',
      narration:'A poeira vermelha se ergue das plataformas; as entradas subterrâneas conduzem a uma rede de túneis.',
      visibleNpcIds:[NPC.bura],checks:[],choices:[
        {id:'nabat-meet-nyn',label:'Conhecer Nyn Kablo e os militantes nos túneis',to:'nabat-contact'},
        {id:'nabat-hull-inspection',label:'Inspecionar o transponder após pousar',to:'krayt-fang-ground-inspection'}]
    },
    'krayt-fang-ground-inspection': {
      episode:1,title:'Inspeção do casco em Ryloth',locationId:LOC.rylosHangar,
      summary:'O transponder não pode ser retirado durante o voo; Mecânica Média após o pouso (SWE02 p.8).',
      narration:'O metal ainda estala com o calor do pouso. Atrás da torre laser há acesso ao casco e aos cabos do rastreador.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'ship-ground-return',label:'Voltar ao hangar sem alegar que o rastreador foi removido',to:'ryloth-safehouse'}]
    },
    'nabat-contact': {
      episode:1,title:'Contato em Nabat',locationId:LOC.nabat,
      summary:'Nyn e B’ura B’an procuram ajuda para os mineiros de New Meen.',
      narration:'Entre passagens subterrâneas, trabalhadores deslocados descrevem a pressão crescente sobre as minas de ryll.',
      visibleNpcIds:[NPC.nyn,NPC.bura],checks:[{skill:'Charme',difficulty:'Média',purpose:'conquistar a confiança dos representantes twi’lek'}],
      choices:[{id:'travel-lylek-den',label:'Viajar para New Meen',to:'lylek-den-ambush'}]
    },
    'lylek-den-ambush': {
      episode:1,title:'Emboscada no covil de lylek',locationId:LOC.lylekDen,
      summary:'O caminho das minas atravessa um antigo covil onde caçadores de Teemo aguardam a tripulação.',
      narration:'A poeira cobre a trilha. O eco de um disparo distante anuncia que os perseguidores encontraram a rota.',
      visibleNpcIds:[],checks:[{skill:'Percepção',difficulty:'Difícil',purpose:'identificar o ataque antes de entrar na passagem'}],
      choices:[{id:'reach-new-meen',label:'Continuar até New Meen após resolver a ameaça',to:'new-meen-miners'}]
    },
    'new-meen-miners': {
      episode:1,title:'A comunidade de New Meen',locationId:LOC.newMeen,
      summary:'Os mineiros precisam de proteção contra os capangas que buscam controlar a extração de ryll.',
      narration:'Ferramentas paradas e moradias danificadas mostram o preço da coerção. Os trabalhadores aguardam uma decisão.',
      visibleNpcIds:[NPC.bura],checks:[{skill:'Liderança',difficulty:'Média',purpose:'organizar a defesa e a retirada dos civis'}],
      choices:[
        {id:'meet-bothan-ota',label:'Prosseguir com Ota sem resolver a situação dos mineiros (não é vitória)',to:'ota-briefing',effects:{newMeenOutcome:'UNRESOLVED'}},
        {id:'confront-drombb-directly',label:'Investigar o acampamento e confrontar Angu Drombb',to:'drombb-cantina'},
        {id:'stay-new-meen-night',label:'Permanecer em New Meen e acompanhar a noite',to:'new-meen-night-raid'},
        {id:'escort-new-meen-evacuees',label:'Organizar retirada e escolta para Nabat',to:'new-meen-evacuation'}
      ]
    },
    // SWE02 printed pp.19–23: the actual New Meen branch, not a generic free success.
    'new-meen-night-raid': {
      episode:1,title:'Perturbação noturna em New Meen',locationId:LOC.newMeen,
      summary:'Uma esquadrilha de capangas embriagados destruiu moradia e condensador. O Mestre confirma a chegada real.',
      narration:'O estrondo de um veículo de esteiras derruba o silêncio das barracas. Os mineiros correm para ver o dano.',
      visibleNpcIds:[NPC.bura],checks:[],
      choices:[
        {id:'follow-drombb-raiders',label:'Seguir até o acampamento após neutralizar o grupo',to:'drombb-cantina',condition:'flag:newMeenNightRaidStopped'},
        {id:'leave-new-meen-after-raid',label:'Recuar e ajudar a retirar os mineiros',to:'new-meen-evacuation'}
      ]
    },
    'drombb-cantina': {
      episode:1,title:'O acampamento de Angu Drombb',locationId:LOC.newMeen,
      summary:'A cerca de uma milha ao sul: cantina, alojamento e cinco veículos de esteiras; grupos sobreviventes protegidos.',
      narration:'A cantina improvisada brilha além dos veículos pesados. Drombb e os capangas restantes guardam o local.',
      visibleNpcIds:[NPC.dromund],checks:[{skill:'Computadores',difficulty:'Difícil',purpose:'recuperar mensagens de Drombb para Thwheek',sourcePage:23}],
      choices:[
        {id:'new-meen-saved-confirmed',label:'Confirmar que Drombb e a ameaça foram neutralizados',to:'new-meen-secured',condition:'flag:drombbThreatNeutralized'},
        {id:'drombb-messages',label:'Recuperar mensagens do escritório (SWE02 p.23)',to:'drombb-cantina',condition:'flag:geoDrombbCheckPassed'},
        {id:'retreat-from-drombb',label:'Recuar sem declarar a comunidade protegida',to:'new-meen-evacuation'},
        {id:'drombb-ota-rescue',label:'Após derrota real, escapar com B’ura e Ota',to:'drombb-ota-rescue',condition:'flag:drombbCaptured'}
      ]
    },
    'drombb-ota-rescue': {
      episode:1,title:'Resgate após derrota diante de Drombb',locationId:LOC.newMeen,
      summary:'SWE02 p.23: B’ura e Ota resgatam os PJs capturados; New Meen NÃO foi libertada.',
      narration:'Os cadeados cedem em silêncio. B’ura e Ota conduzem o grupo para longe da cantina, enquanto Drombb permanece no controle.',
      visibleNpcIds:[NPC.bura,NPC.ota],checks:[],
      choices:[{id:'ota-after-drombb-rescue',label:'Ouvir a proposta de Ota após a derrota',to:'ota-briefing'}]
    },
    'new-meen-secured': {
      episode:1,title:'New Meen protegida',locationId:LOC.newMeen,
      summary:'A ameaça foi neutralizada em confronto verificável. Os mineiros permanecem; nenhuma recompensa automática.',
      narration:'A poeira assenta nas vias da mina. A noite pode voltar a ser silenciosa, mas a comunidade ainda precisará se reerguer.',
      visibleNpcIds:[NPC.bura],checks:[],
      choices:[{id:'ota-after-new-meen-defense',label:'Seguir para o encontro com Ota',to:'ota-briefing'}]
    },
    'new-meen-evacuation': {
      episode:1,title:'Retirada de New Meen',locationId:LOC.newMeen,
      summary:'A comunidade não foi comprovadamente protegida; a rota de escolta até Nabat permanece sujeita ao Mestre.',
      narration:'Os moradores recolhem o que podem transportar. Nos caminhos de volta, a ameaça de Teemo ainda existe.',
      visibleNpcIds:[NPC.bura],checks:[],
      choices:[
        {id:'ota-after-new-meen-escort',label:'Chegar a Nabat com os civis escoltados (favor de B’ura, não libertação)',to:'ota-briefing',condition:'flag:newMeenEscortArrived'},
        {id:'ota-after-new-meen-withdrawal',label:'Seguir sem completar escolta (nenhum favor concedido)',to:'ota-briefing'}]
    },
    'ota-briefing': {
      episode:2,title:'Informações de Ota',locationId:LOC.nabat,
      summary:'O Bothan revela as compras de equipamentos de Teemo e suas negociações com Geonosianos.',
      narration:'Ota aguarda a saída dos demais antes de abrir seu arquivo. A oportunidade de enfraquecer Teemo depende de contatos perigosos.',
      visibleNpcIds:[NPC.ota],checks:[{skill:'Manha',difficulty:'Média',purpose:'avaliar a credibilidade e as ligações da informação'}],
      choices:[{id:'follow-ota-geonosis',label:'Partir para Geonosis',to:'geonosis-approach'}]
    },
    'piddock-party': {
      episode:2,title:'Recepção de Duke Piddock',locationId:LOC.piddockParty,
      summary:'A festa do duque permite colher provas das transações e da rivalidade entre fornecedores.',
      narration:'Em meio a convidados e mercadores, cada conversa pode revelar um novo elo entre Teemo e a indústria de armas.',
      visibleNpcIds:[NPC.piddock],checks:[{skill:'Charme',difficulty:'Difícil',purpose:'conhecer os planos de droides de Teemo',sourcePage:34}],
      choices:[
        {id:'piddock-kubaz',label:'Perguntar por que Piddock rompeu com Teemo',to:'piddock-party',condition:'!flag:geo_kubaz'},
        {id:'piddock-b1',label:'Registrar planos de droides após Charme Difícil',to:'piddock-party',condition:'flag:geoPiddockCharmPassed'},
        {id:'piddock-identify-shell',label:'Pedir identificação da carapaça encontrada',to:'piddock-party',condition:'flag:geo_shell'},
        {id:'piddock-agent-visit',label:'Encontrar agente de Piddock no cargueiro para pagar e receber armas',to:'piddock-freighter-handoff',condition:'flag:piddockTradeAgreed'},
        {id:'piddock-to-dimmock',label:'Ir à audiência pontuada de Dimmock',to:'dimmock-official-audience'},
        {id:'piddock-hire-vios',label:'Embarcar com os Vio após pagamento verificado (SWE02 p.33)',to:'lucky-guess-boarding',condition:'flag:vioHired'},
        {id:'speak-with-dimmock',label:'Buscar o apoio de Duke Dimmock (rota legada)',to:'dimmock-negotiation'}]
    },
    'piddock-freighter-handoff':{
      episode:2,title:'Entrega de armas no cargueiro',locationId:LOC.geonoSurface,
      summary:'SWE02 p.33: acordo verbal sem débito na festa; agente cobra e entrega armas junto ao cargueiro.',
      narration:'O representante de Piddock aguarda perto do cargueiro com as caixas lacradas e a cobrança acertada.',
      visibleNpcIds:[],checks:[],choices:[{id:'piddock-agent-return',label:'Retornar à festa de Piddock',to:'piddock-party'}]
    },
    'dimmock-negotiation': {
      episode:2,title:'A posição de Duke Dimmock',locationId:LOC.piddockParty,
      summary:'Dimmock desconfia de Teemo, mas só ajudará se as evidências forem convincentes.',
      narration:'O duque escuta em silêncio. Uma acusação precipitada pode fechar a única via de apoio diplomático.',
      visibleNpcIds:[NPC.dimmock],checks:[{skill:'Negociação',difficulty:'Difícil',purpose:'persuadir o duque com os registros reunidos'}],
      choices:[{id:'geonosis-final-evidence',label:'Reunir os documentos antes do retorno',to:'geonosis-surface'},
        {id:'dimmock-audience',label:'Iniciar audiência oficial de Dimmock',to:'dimmock-official-audience'}]
    },
    'dimmock-official-audience':{
      episode:2,title:'Audiência oficial de Duke Dimmock',locationId:LOC.geonoSurface,
      summary:'SWE02 p.36: pontuação auditável com fatos, conduta e testes reais.',
      narration:'O duque exige fatos concretos e ouve cada argumento com cautela.',visibleNpcIds:[NPC.dimmock],
      checks:[{skill:'Charme',difficulty:'Média',purpose:'ser cortês com Dimmock',sourcePage:36},
        {skill:'Coerção',difficulty:'Formidável',purpose:'ameaçar o duque: falha custa dois pontos',sourcePage:36}],
      choices:[
        {id:'dimmock-anti-empire',label:'Declarar oposição ao Império (+1)',to:'dimmock-official-audience'},
        {id:'dimmock-loyal-empire',label:'Declarar lealdade ao Império (-1)',to:'dimmock-official-audience'},
        {id:'dimmock-rude',label:'Ser rude com o duque (-1)',to:'dimmock-official-audience'},
        {id:'dimmock-kubaz',label:'Apresentar espião Kubaz (+1)',to:'dimmock-official-audience',condition:'flag:geo_kubaz'},
        {id:'dimmock-b1',label:'Apresentar plano de droides B1 (+1)',to:'dimmock-official-audience',condition:'flag:geo_b1'},
        {id:'dimmock-shell',label:'Apresentar carapaça identificada (+1)',to:'dimmock-official-audience',condition:'flag:geo_identifiedShell'},
        {id:'dimmock-sivor',label:'Informar a morte de Sivor pelo Kubaz (+1)',to:'dimmock-official-audience',condition:'flag:geo_messages'},
        {id:'dimmock-messages',label:'Apresentar comunicações de Drombb (+1)',to:'dimmock-official-audience',condition:'flag:geo_messages'},
        {id:'dimmock-finish',label:'Concluir argumentos e ir ao gantry AA7',to:'gantry-aa7'}]
    },
    'gantry-aa7':{
      episode:2,title:'Gantry AA7 — Lucky Guess',locationId:LOC.geonoSurface,
      summary:'Cinco, três ou zero soldados segundo os pontos de Dimmock; Vrixx’tt chega em qualquer caso (SWE02 p.37).',
      narration:'Os contêineres formam barricadas ao lado do cargueiro. Um Gand observa a passarela.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'board-lucky-guess',label:'Embarcar após combate, fuga ou resgate confirmado',to:'lucky-guess-boarding',condition:'flag:geoGantryCleared'},
        {id:'gantry-retreat',label:'Retirar-se sem vitória',to:'geonosis-surface'}]
    },
    'lucky-guess-boarding':{
      episode:2,title:'A bordo da Lucky Guess',locationId:LOC.geonoSurface,
      summary:'Os irmãos Vio mantêm a propriedade do cargueiro; destino Mos Shuuta (SWE02 p.37).',
      narration:'Os motores se acendem e a rampa do cargueiro se fecha.',visibleNpcIds:[],checks:[],
      choices:[{id:'lucky-guess-to-tatooine',label:'Seguir para Mos Shuuta',to:'tatooine-infiltration'}]
    },
    'rylos-escape-aftermath': {
      episode: 1,
      title: 'Após a fuga de Tatooine',
      locationId: LOC.rylosHangar,
      summary: 'A tripulação escapou de Mos Shuuta na Krayt Fang, mas Teemo o Hutt colocou caçadores de recompensa na sua cola.',
      narration: 'O hiperespaço desvanece e o cargueiro entra em órbita de Ryloth. Teemo não deixará o insulto passar em branco; a caçada pelo grupo acabou de começar.',
      visibleNpcIds: [],
      checks: [
        { skill: 'Astrogação', difficulty: 'Média', purpose: 'verificar registros de navegação e despistar rastreadores' },
        { skill: 'Percepção', difficulty: 'Média', purpose: 'inspecionar o cargueiro em busca de sinalizadores clandestinos' }
      ],
      choices: [
        { id: 'dock-ryloth', label: 'Aterrissar nos hangares subterrâneos de Ryloth', to: 'ryloth-safehouse' },
        {id:'inspect-krayt-fang',label:'Examinar registros e rastreadores antes do pouso',to:'krayt-fang-logs'}
      ]
    },
    'ryloth-safehouse': {
      episode: 1,
      title: 'Refúgio em Nova Ryloth',
      locationId: LOC.rylosHangar,
      summary: 'Encontro com contatos Twi\'lek para negociar peças e desativar o controle remoto da Krayt Fang.',
      narration: 'A umidade e o calor das cavernas de Ryloth contrastam com o ar reciclado da nave. Contatos locais aceitam ajudar, mas alertam que agentes de Teemo já desembarcaram no setor.',
      visibleNpcIds: [NPC.thwheel],
      checks: [
        { skill: 'Negociação', difficulty: 'Média', purpose: 'obter códigos de cancelamento e suprimentos' },
        { skill: 'Manha', difficulty: 'Difícil', purpose: 'descobrir quem está contratando pistoleiros na cidade' }
      ],
      choices: [
        { id: 'head-to-geonosis', label: 'Seguir para Geonosis em busca de aliados', to: 'geonosis-approach' },
        {id:'help-new-meen',label:'Investigar a situação dos mineiros de New Meen',to:'nabat-contact'},
        {id:'ship-inspect-hull',label:'Inspecionar o casco e tentar retirar o transponder (p.8)',to:'krayt-fang-ground-inspection'}
      ]
    },
    'geonosis-approach': {
      episode: 2,
      title: 'Aproximação a Geonosis',
      locationId: LOC.geonoOrbit,
      summary: 'A viagem até os anéis de Geonosis para negociar com um antigo fabricante de droides.',
      narration: 'Os anéis de asteroides de Geonosis cobrem a atmosfera avermelhada. Caças piratas e patrulhas da colmeia guardam os desfiladeiros de entrada.',
      visibleNpcIds: [],
      checks: [
        { skill: 'Pilotagem (Espacial)', difficulty: 'Difícil', purpose: 'manobrar entre os detritos dos anéis' },
        { skill: 'Computadores', difficulty: 'Média', purpose: 'interceptar comunicações da superfície' }
      ],
      choices: [
        { id: 'enter-canyon', label: 'Descer até os desfiladeiros da colmeia', to: 'geonosis-surface' },
        {id:'attend-piddock-party',label:'Buscar informações na recepção de Duke Piddock',to:'piddock-party'}
      ]
    },
    'geonosis-surface': {
      episode: 2,
      title: 'As Torres da Colmeia',
      locationId: LOC.geonoSurface,
      summary: 'Exploração dos laboratórios subterrâneos e negociação com os guerreiros Geonosianos.',
      narration: 'O ar é espesso e o eco dos insetoides reverbera pelas cavernas de espirais de pedra. Um arquiteto geonosiano pode fornecer provas da traição de Teemo perante o Conselho Hutt.',
      visibleNpcIds: [NPC.vorn],
      checks: [
        { skill: 'Charme', difficulty: 'Difícil', purpose: 'aplacar a desconfiança dos guerreiros geonosianos' },
        { skill: 'Mecânica', difficulty: 'Média', purpose: 'recuperar registros de transações danificados' }
      ],
      choices: [
        { id: 'prepare-tatooine-return', label: 'Retornar a Tatooine para confrontar Teemo', to: 'tatooine-infiltration' }
      ]
    },
    // SWE02 printed pp.41–46. These branches do not bypass real checks or
    // declare Teemo defeated merely because the player entered a room.
    'palace-front-gate': {
      episode:3,title:'Portões fortificados de Teemo',locationId:LOC.teemoPalace,
      summary:'Duas sentinelas Gamorreanas e dois capangas vigiam o acesso. Entrada furtiva pela porta frontal é impossível.',
      narration:'Dois guardas maciços vigiam o portão blindado. A aproximação será percebida; a decisão é como se apresentar.',
      visibleNpcIds:[],checks:[
        {skill:'Enganação',difficulty:'Média',purpose:'alegar que veio quitar as dívidas',sourcePage:41},
        {skill:'Enganação',difficulty:'Difícil',purpose:'sustentar um disfarce',sourcePage:41}
      ],choices:[
        {id:'palace-gate-debt-bluff',label:'Entrar como devedor após Enganação Média bem-sucedida',to:'palace-reception',condition:'flag:palaceGateBluffPassed'},
        {id:'palace-gate-disguise',label:'Pedir audiência disfarçado após Enganação Difícil bem-sucedida',to:'palace-disguise-audience',condition:'flag:palaceDisguisePassed'},
        {id:'palace-gate-retreat',label:'Retirar-se sem forçar o portão',to:'laoth-retreat'}
      ]
    },
    'palace-disguise-audience': {
      episode:3,title:'Pedido de audiência',locationId:LOC.teemoPalace,
      summary:'O disfarce precisa ser acompanhado de um pedido convincente de audiência.',
      narration:'O porteiro hesita; uma palavra inadequada desfará a identidade improvisada.',
      visibleNpcIds:[],checks:[{skill:'Charme',difficulty:'Média',purpose:'conseguir audiência após disfarce',sourcePage:41}],
      choices:[{id:'palace-disguise-approved',label:'Entrar após Charme Média bem-sucedido',to:'palace-reception',condition:'flag:palaceAudiencePassed'},
        {id:'palace-disguise-retreat',label:'Retirar-se',to:'laoth-retreat'}]
    },
    'palace-reception': {
      episode:3,title:'Recepção do palácio',locationId:LOC.teemoPalace,
      summary:'Mesas e assentos oferecem cobertura; os aposentos podem ser explorados sem combate automático.',
      narration:'A recepção parece confortável demais para um complexo fortificado. Há portas para o salão e áreas de serviço.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'palace-enter-hallway',label:'Explorar o grande corredor e suas obras de arte',to:'palace-grand-hallway'},
        {id:'palace-enter-gladiators',label:'Visitar as celas dos gladiadores (suborno ou Coerção)',to:'palace-gladiator-cells'},
        {id:'palace-enter-workshop',label:'Investigar oficina e recebimento de carga',to:'palace-workshop'},
        {id:'palace-enter-kitchen',label:'Investigar cozinha e depósitos',to:'palace-kitchen'},
        {id:'palace-enter-bar',label:'Investigar o bar e conversar com o droide cozinheiro',to:'palace-cantina-bar'},
        {id:'palace-enter-pad',label:'Inspecionar o canhão da plataforma de pouso',to:'palace-landing-pad'},
        {id:'palace-enter-spy',label:'Esperar oportunidade de conversar reservadamente com o músico Bith',to:'palace-spy-interview'},
        {id:'palace-enter-comms',label:'Investigar central de comunicações',to:'palace-communications'},
        {id:'palace-enter-throne',label:'Dirigir-se à sala do trono',to:'teemo-showdown'},
        {id:'palace-jabba-resolution',label:'Concluir pela intervenção de Jabba após provas e Anatta',to:'laoth-epilogue',condition:'flag:teemoPeacefullyNeutralized'}]
    },
    'palace-grand-hallway': {
      episode:3,title:'Grande corredor do palácio',locationId:LOC.teemoPalace,
      summary:'Galeria de quadros falsificados, holoescultura de Teemo e mosaico. Quadros volumosos podem render cerca de 450 créditos numa venda posterior — nenhuma moeda é criada aqui (SWE02 p.42).',
      narration:'O corredor veste veludo vermelho. Molduras douradas imensas cercam pinturas pouco autênticas, enquanto uma holoescultura do Hutt flutua acima do mosaico.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'palace-hallway-return',label:'Voltar à recepção',to:'palace-reception'},
        {id:'palace-hallway-throne',label:'Seguir ao trono pelo grande corredor',to:'teemo-showdown'}]
    },
    'palace-gladiator-cells': {
      episode:3,title:'Celas dos quatro gladiadores',locationId:LOC.teemoPalace,
      summary:'Quatro gladiadores usam a ficha de capangas (p.21); Charme não funciona. Podem ser subornados por 1.000 créditos ou coagidos contra Disciplina (p.44).',
      narration:'Quatro lutadores esperam entre grades e armas de treino. O dinheiro e a força os impressionam mais que palavras gentis.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'palace-gladiators-return',label:'Voltar à recepção',to:'palace-reception'},
        {id:'palace-gladiators-to-throne',label:'Seguir ao trono sem inventar ajuda',to:'teemo-showdown'}]
    },
    'palace-cantina-bar': {
      episode:3,title:'Bar do palácio',locationId:LOC.teemoPalace,
      summary:'Droide COO pacífico, cobertura +2 revés contra alvos atrás do bar; explosão com duas Vantagens requer executor de atordoamento certificado (p.44).',
      narration:'O droide prepara bebidas atrás de um balcão pesado, longe do centro do trono.',
      visibleNpcIds:[],checks:[],choices:[{id:'palace-bar-return',label:'Voltar à recepção',to:'palace-reception'},
        {id:'palace-bar-throne',label:'Voltar ao salão do trono',to:'teemo-showdown'}]
    },
    'palace-landing-pad': {
      episode:3,title:'Plataforma e canhão laser leve',locationId:LOC.teemoPalace,
      summary:'Artilheiro com Artelharia 1; canhão veicular de dano 4, alcance Curto, silhueta 3, imóvel (p.44). A ação de tiro requer combate veicular real.',
      narration:'O canhão de plataforma varre o horizonte enquanto os guardas observam o tráfego.',
      visibleNpcIds:[],checks:[],choices:[{id:'palace-pad-return',label:'Retornar à recepção',to:'palace-reception'}]
    },
    'palace-spy-interview': {
      episode:3,title:'Entrevista reservada com Smooth Banjaxx Wab',locationId:LOC.teemoPalace,
      summary:'O músico Bith deixa o salão e pode ser confrontado sem combate; seu relato prova espionagem de Jabba (pp.44–45).',
      narration:'O músico atravessa o corredor em direção ao refresher, longe da atenção dos guardas.',
      visibleNpcIds:[],checks:[],choices:[{id:'palace-spy-return',label:'Voltar ao salão',to:'palace-reception'}]
    },
    'palace-workshop': {
      episode:3,title:'Oficina e carga',locationId:LOC.teemoPalace,
      summary:'Três droides B1 inoperantes e um diário de trabalho provam o projeto de engenharia reversa de Teemo (p.44).',
      narration:'Ferramentas e peças de droides ocupam bancadas. Um datapad repousa ao lado de chassis incompletos.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'palace-record-b1-worklog',label:'Examinar os droides e recolher o diário técnico',to:'palace-workshop-evidence',effects:{b1ReverseEngineeringEvidence:true}},
        {id:'palace-workshop-return',label:'Voltar à recepção',to:'palace-reception'}]
    },
    'palace-workshop-evidence': {
      episode:3,title:'Registro dos droides B1',locationId:LOC.teemoPalace,
      summary:'O diário comprova que Teemo tenta reproduzir droides de combate; a evidência não é recompensa.',
      narration:'As anotações mostram tentativas sucessivas de copiar a arquitetura dos B1.',
      visibleNpcIds:[],checks:[],choices:[{id:'palace-worklog-to-comms',label:'Seguir para a central de comunicações',to:'palace-communications'},
        {id:'palace-worklog-to-throne',label:'Seguir para o trono',to:'teemo-showdown'}]
    },
    'palace-kitchen': {
      episode:3,title:'Cozinha e depósitos',locationId:LOC.teemoPalace,
      summary:'Um teste Difícil de Percepção pode revelar caixas de ryll sob rótulos de alimento (p.44).',
      narration:'Entre barris de provisões e caixotes, dois volumes parecem pesados demais para conter ensopado.',
      visibleNpcIds:[],checks:[{skill:'Percepção',difficulty:'Difícil',purpose:'identificar caixas de ryll',sourcePage:44}],
      choices:[{id:'palace-kitchen-reveal-ryll',label:'Registrar evidência do ryll encontrado',to:'palace-kitchen-evidence',condition:'flag:palaceKitchenFoundRyll'},
        {id:'palace-kitchen-return',label:'Voltar à recepção',to:'palace-reception'}]
    },
    'palace-kitchen-evidence': {
      episode:3,title:'Carga clandestina de ryll',locationId:LOC.teemoPalace,
      summary:'O conteúdo das caixas comprova participação no comércio de ryll.',
      narration:'A tampa revela uma carga que não tem relação com os alimentos declarados.',
      visibleNpcIds:[],checks:[],choices:[{id:'palace-ryll-to-comms',label:'Seguir para comunicações',to:'palace-communications'}]
    },
    'palace-communications': {
      episode:3,title:'Central de comunicações',locationId:LOC.teemoPalace,
      summary:'Terminais com segurança: +1 revés, +1 se o alarme foi disparado; duas ameaças bloqueiam acesso (p.45).',
      narration:'Terminais da HoloNet ocupam a sala. Luzes de acesso oscilam junto a indicadores de segurança.',
      visibleNpcIds:[],checks:[
        {skill:'Computadores',difficulty:'Fácil',purpose:'acessar câmeras',sourcePage:45},
        {skill:'Computadores',difficulty:'Média',purpose:'desativar alarmes e comunicação externa',sourcePage:45},
        {skill:'Computadores',difficulty:'Difícil',purpose:'obter registros financeiros do ryll',sourcePage:45},
        {skill:'Computadores',difficulty:'Difícil',purpose:'obter esquemas dos droides B1',sourcePage:45},
        {skill:'Computadores',difficulty:'Difícil',purpose:'obter gravações da espionagem de Jabba',sourcePage:45}
      ],choices:[{id:'palace-comms-to-throne',label:'Prosseguir ao trono com as provas obtidas',to:'teemo-showdown'},
        {id:'palace-comms-to-reception',label:'Voltar à recepção',to:'palace-reception'},
        {id:'palace-comms-peaceful-ending',label:'Encerrar a perseguição após o dossiê enviado a Jabba',to:'laoth-epilogue',condition:'flag:teemoPeacefullyNeutralized'}]
    },
    'mos-shuuta-electrogate':{
      episode:3,title:'Eletroportão de Mos Shuuta',locationId:LOC.teemoPalace,
      summary:'SWE02 p.40: portão energizado sem sentinelas. Computadores Difícil para desligá-lo; quatro Gamorreanos são chamados do quartel e não poderão reforçar Teemo.',
      narration:'O campo elétrico vibra entre os pilares. Nenhuma sentinela aparece, mas um alarme automático vigia os controles.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'mos-gate-return',label:'Retornar ao planejamento de Mos Shuuta',to:'tatooine-infiltration'},
        {id:'mos-gate-guards',label:'Aproximar-se do palácio, sem ignorar os quatro guardas alertados',to:'palace-front-gate',condition:'flag:electrogateBarracksDiverted'}]
    },
    'tatooine-infiltration': {
      episode: 3,
      title: 'Retorno a Mos Shuuta',
      locationId: LOC.teemoPalace,
      summary: 'Infiltração no palácio de Teemo para apresentar as provas ou liquidar o contrato de caça.',
      narration: 'Os sóis gêmeos queimam os penhascos de Mos Shuuta. Gamorreanos guardam as portas do palácio enquanto a corte de Teemo se deleita em banquetes e negócios escusos.',
      visibleNpcIds: [NPC.teemo, NPC.dromund],
      checks: [
        { skill: 'Furtividade', difficulty: 'Difícil', purpose: 'infiltrar-se pelos dutos de manutenção' },
        { skill: 'Coerção', difficulty: 'Difícil', purpose: 'intimidar os capangas de Teemo a baixarem as armas' }
      ],
      choices: [
        { id: 'confront-teemo', label: 'Forçar o confronto frontal com os guardas e Teemo', to: 'teemo-showdown',effects:{palaceEntryMethod:'FRONTAL_ASSAULT'} },
        { id: 'mos-gate-investigate', label: 'Investigar o eletroportão e o alarme do quartel', to: 'mos-shuuta-electrogate' },
        { id: 'approach-fortified-gate', label: 'Tentar acesso pelos portões com diálogo ou disfarce', to: 'palace-front-gate' },
        { id: 'palace-entry-retreat', label: 'Desistir do ataque ao palácio', to: 'laoth-retreat' }
      ]
    },
    'teemo-showdown': {
      episode: 3,
      title: 'O Acerto de Contas com Teemo',
      locationId: LOC.teemoPalace,
      summary: 'O clímax no salão do trono: o grupo expõe Teemo perante emissários Hutt ou elimina sua guarda.',
      narration: 'Teemo gargalha na sua plataforma flutuante, mas seus olhos revelam apreensão conforme as provas e armas do grupo são reveladas diante de todos.',
      visibleNpcIds: [NPC.teemo],
      checks: [
        { skill: 'Percepção', difficulty: 'Média', purpose: 'identificar a corrente vulnerável do lustre', sourcePage: 46 }
      ],
      choices: [
        { id: 'withdraw-from-palace', label: 'Retirar-se sem derrubar Teemo', to: 'laoth-retreat' },
        { id: 'teemo-resolved', label: 'Confirmar a derrota de Teemo após combate adjudicado', to: 'laoth-epilogue', condition:'flag:teemoDefeated' },
        { id: 'teemo-peaceful-ending', label: 'Encerrar pelo acordo comprovado com Teemo ou pela intervenção de Jabba', to: 'laoth-epilogue', condition:'flag:teemoPeacefullyNeutralized' }
      ]
    },
    'laoth-retreat': {
      episode:3,title:'Retirada de Mos Shuuta',locationId:LOC.teemoPalace,
      summary:'A tripulação escapou do palácio, mas a perseguição de Teemo continua.',
      narration:'Os guardas retomam os postos. A tripulação deixa o palácio sem provas de que Teemo perdeu o poder.',
      visibleNpcIds:[],checks:[],choices:[]
    },
    'laoth-epilogue': {
      episode: 3,
      title: 'Epílogo: Liberdade na Orla Exterior',
      locationId: LOC.teemoPalace,
      summary: 'Teemo morreu em confronto documentado, aceitou acordo comprovado ou foi removido por Jabba; consequências dependem das provas.',
      narration: 'O salão esvazia enquanto a notícia do desfecho corre pelas vielas de Mos Shuuta. O futuro dependerá das provas e dos acordos realmente obtidos.',
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
  if (c==='flag:shipDepartureVerified') return Boolean(progress.flags?.shipCoursePlotted&&progress.flags?.shipBuraFound);
  if (c==='flag:shipThwheekCombatStarted') return progress.currentNodeId==='thwheek-orbit'&&progress.flags?.shipThwheekInterceptionRequired===true&&!progress.flags?.shipThwheekCombatStarted;
  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);
  if (c.startsWith('!flag:')) return !Boolean(progress.flags?.[c.slice(6)]);
  if (c.startsWith('encounter-resolved:')) return (progress.encounterHistory || []).some(x => x.encounterId === c.slice(19) && x.outcome === 'adversaries_defeated');
  return false;
}

function assertNewMeenCausality(progress) {
  if (!progress || progress.id!==ID) return true;
  const history=Array.isArray(progress.encounterHistory)?progress.encounterHistory:[];
  const verified=id=>history.some(r=>r.encounterId===id&&r.outcome==='adversaries_defeated'&&r.combatProof?.eliminated===true&&
    Array.isArray(r.combatProof.enemyIds)&&r.combatProof.enemyIds.length===r.combatProof.groupCount&&
    new Set(r.combatProof.enemyIds).size===r.combatProof.groupCount);
  if (progress.flags?.newMeenNightRaidStopped===true&&!verified('laoth-new-meen-night-raid'))
    throw new Error('New Meen: vitória noturna sem registro de combate verificável.');
  if (progress.flags?.drombbThreatNeutralized===true&&!verified('laoth-drombb-cantina'))
    throw new Error('New Meen: Drombb neutralizado sem registro de combate verificável.');
  if(progress.flags?.newMeenOutcome==='SECURED' && !verified('laoth-drombb-cantina'))
    throw new Error('New Meen: local declarado protegido sem combate registrado.');
  return true;
}

function assertTeemoCausality(progress) {
  if (!progress || progress.id!==ID) return true;
  validateSWE02Act3({publishedAdventure:progress});
  validateSWE02Palace({publishedAdventure:progress});
  if(progress.flags?.teemoPeacefullyNeutralized===true){
    if(!progress.act3Resolution||progress.flags?.teemoDefeated===true)throw Error('Teemo: paz sem recibo de Anatta.');
    return true;
  }
  if (progress.flags?.teemoDefeated!==true && progress.currentNodeId!=='laoth-epilogue') return true;
  const verified=(progress.encounterHistory||[]).some(r=>r.encounterId==='teemo-palace-guards'&&r.outcome==='adversaries_defeated'&&
    r.combatProof?.teemoIncapacitated===true&&r.combatProof?.eliminated===true&&
    Array.isArray(r.combatProof.enemyIds)&&new Set(r.combatProof.enemyIds).size===r.combatProof.enemyIds.length&&r.combatProof.enemyIds.length===r.combatProof.groupCount);
  if(!verified)throw Error('Teemo: derrota ou epílogo sem combate efetivo e recibo íntegro.');
  return true;
}

function assertPalaceCausality(progress) {
  if (!progress || progress.id!==ID) return true;
  const h=Array.isArray(progress.checkHistory)?progress.checkHistory:[];
  const has=(node,index,pred)=>h.some(r=>r.nodeId===node&&r.checkIndex===index&&r.success===true&&(!pred||pred(r)));
  const flags=progress.flags||{};
  for(const [flag,node,index] of [
    ['palaceGateBluffPassed','palace-front-gate',0],
    ['palaceDisguisePassed','palace-front-gate',1],
    ['palaceAudiencePassed','palace-disguise-audience',0],
    ['palaceKitchenFoundRyll','palace-kitchen',0],
    ['ryllFinancialEvidence','palace-communications',2],
    ['bithSpyEvidence','palace-communications',4]
  ])if(flags[flag]===true&&!has(node,index))throw Error(`SWE02: ${flag} sem recibo de perícia verificado.`);
  if(flags.palaceComputersLocked===true && !h.some(r=>r.nodeId==='palace-communications'&&Number(r.netAdvantage)<=-2))
    throw Error('SWE02: terminal bloqueado sem duas ameaças documentadas.');
  return true;
}

function playerNode(node, progress) {
  if (!node) return null;
  const consequence=progress.currentNodeId==='laoth-epilogue'?swe02Act3Consequences(progress):null;
  return {
    id: progress.currentNodeId,
    episode: node.episode,
    title: node.title,
    locationId: node.locationId,
    summary: consequence?.teemoAliveDeal?'Teemo continua vivo e prometeu deixar o grupo em paz mediante provas concretas.':consequence?.teemoRemovedByJabba?'Anatta apresentou as provas; Jabba retirou Teemo de circulação, sem morte confirmada.':node.summary,
    narration: consequence?.teemoAliveDeal?'Teemo ainda respira, mas agora sabe que seus segredos estão nas mãos da tripulação. O acordo afasta a caçada por enquanto.':consequence?.teemoRemovedByJabba?'O poder de Teemo se desfaz sob a intervenção de Jabba; nenhuma notícia confirma sua morte.':node.narration,
    visibleNpcIds: [...(node.visibleNpcIds || [])],
    checks: clone(node.checks || []),
    choices: (node.choices || []).filter(c => conditionOk(c, progress) &&
      !(FACTS[c.id]&&(progress.geonosisEvidence||[]).some(x=>x.choiceId===c.id)) &&
      !(ARGUMENTS[c.id]&&(progress.geonosisArguments||[]).some(x=>x.choiceId===c.id)) &&
      !(['dimmock-anti-empire','dimmock-loyal-empire'].includes(c.id)&&(progress.geonosisArguments||[]).some(x=>['antiEmpire','loyalEmpire'].includes(x.key)))).map(c => ({ id: c.id, label: c.label }))
  };
}

function baseProgress({ rewardsApplied = false } = {}) {
  return {
    id: ID,
    title: LONG_ARM_OF_THE_HUTT.title,
    status: 'ACTIVE',
    currentNodeId: LONG_ARM_OF_THE_HUTT.startNodeId,
    visitedNodeIds: [LONG_ARM_OF_THE_HUTT.startNodeId],
    flags: {},
    clocks: clone(LONG_ARM_OF_THE_HUTT.initialClocks),
    checkHistory: [],geonosisEvidence:[],geonosisArguments:[],
    consequencesApplied: false,
    startedAt: now(),
    updatedAt: now(),
    completedAt: null,
    rewardsApplied: Boolean(rewardsApplied),
    source: SOURCE,
    implementation: clone(LONG_ARM_OF_THE_HUTT.implementation)
  };
}

export function startLongArmOfTheHutt(world) {
  const next = clone(world || {});
  next.publishedAdventure = baseProgress();
  const p = next.publishedAdventure;
  return {
    world: next,
    progress: clone(p),
    node: playerNode(LONG_ARM_OF_THE_HUTT.nodes[p.currentNodeId], p)
  };
}

export function advanceLongArmOfTheHutt(world, choiceId) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');
  assertNewMeenCausality(p);
  assertTeemoCausality(p);
  assertPalaceCausality(p);
  validateGeonosis(p);
  validateShip(p);
  validateThwheek(p);
  validateNabat(p);
  validateSWE02Social(p);
  validateNewMeenP242(p);
  validateNewMeenSafe(p);
  validateNewMeenP243(next);
  validateSWE02Anatta(next);
  validateSWE02Act2Contracts(next);
  if(Boolean(p.flags?.piddockTradeAgreed)!==Boolean((p.piddockArmsAgreements||[]).length))throw Error('SWE02: acesso ao agente sem acordo comercial.');
  const node = LONG_ARM_OF_THE_HUTT.nodes[p.currentNodeId];
  const choice = (node?.choices || []).find(c => c.id === choiceId);
  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha inválida.');
  if(p.currentNodeId==='piddock-party'&&['piddock-kubaz','piddock-b1','piddock-identify-shell'].includes(choiceId)){
    const tracked=askSWE02Piddock(next,{topic:choiceId});
    p.piddockQuestionReceipts=tracked.world.publishedAdventure.piddockQuestionReceipts;
    p.piddockConversationClosed=tracked.world.publishedAdventure.piddockConversationClosed;
  }
  if (choiceId === 'teemo-resolved') assertTeemoCausality({...p,flags:{...p.flags,teemoDefeated:true}});
  if(['teemo-peaceful-ending','palace-jabba-resolution','palace-comms-peaceful-ending'].includes(choiceId)&&!p.act3Resolution)throw Error('SWE02: acordo ou intervenção de Jabba não comprovados.');
  if(choiceId==='ship-thwheek-to-nabat'&&!p.shipThwheekReceipt)throw Error('SWE02: sem prova de combate espacial.');
  if (p.activeEncounterId && ['new-meen-night-raid','drombb-cantina','teemo-showdown','gantry-aa7'].includes(p.currentNodeId)) throw new Error('Encerre o combate de New Meen antes de sair desta cena.');
  p.flags = { ...(p.flags || {}), ...(choice.effects || {}) };
  recordGeonosisChoice(p,choiceId,p.currentNodeId);
  if(choiceId==='dock-ryloth'||choiceId==='finish-ship-inspection')recordShipChoice(p,'ship-legacy-landing');
  else recordShipChoice(p,choiceId);
  if(choiceId==='ship-depart-verified')p.flags.shipThwheekSkipped=p.flags.shipThwheekInterceptionRequired===false;
  if (choice.to === 'palace-kitchen-evidence') p.flags.ryllTradeEvidence=true;
  if (choice.to === 'new-meen-evacuation'&&p.flags.newMeenOutcome!=='LOST') p.flags.newMeenOutcome = 'EVACUATING';
  if(choiceId==='ota-after-new-meen-withdrawal'&&!p.flags.newMeenEscortArrived&&p.newMeenEscort?.status!=='ABANDONED'){p.flags.newMeenEscortAbandoned=true;
    if(!p.newMeenEscort)p.newMeenEscort={status:'ABANDONED',adults:20,childrenCount:null,origin:'laoth.loc.new_meen',destination:'laoth.loc.nabat',startedAtTick:Number(next.clock?.tick)||0,departureTick:null,arrivalTick:null,gmApprovedTravelTicks:null,significantFavor:false};
    else {p.newMeenEscort.status='ABANDONED';p.newMeenEscort.significantFavor=false;}
    p.newMeenEscortLog=[...(p.newMeenEscortLog||[]),{type:'NEW_MEEN_ESCORT_ABANDONED',source:{productCode:'SWE02',printedPages:[20,21],authority:'OFFICIAL_PDF_TEXT_VERIFIED'},nodeId:'new-meen-evacuation',tick:Number(next.clock?.tick)||0,significantFavor:false}];}
  if (choice.to === 'new-meen-secured') {
    if (p.flags.drombbThreatNeutralized !== true) throw new Error('A vitória em New Meen exige evidência de confronto encerrado.');
    p.flags.newMeenOutcome = 'SECURED';
  }
  const destination=choice.to;
  p.currentNodeId = destination;
  if (!p.visitedNodeIds.includes(destination)) p.visitedNodeIds.push(destination);
  const target = LONG_ARM_OF_THE_HUTT.nodes[destination];
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

export function longArmOfTheHuttStatus(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) {
    return {
      available: true,
      active: false,
      catalog: { id: ID, title: LONG_ARM_OF_THE_HUTT.title, campaignModes: [...LONG_ARM_OF_THE_HUTT.campaignModes], rulesProfile: LONG_ARM_OF_THE_HUTT.rulesProfile }
    };
  }
  assertNewMeenCausality(p);
  assertTeemoCausality(p);
  assertPalaceCausality(p);
  validateGeonosis(p);
  validateShip(p);
  validateThwheek(p);
  validateNabat(p);
  validateSWE02Social(p);
  validateNewMeenP242(p);
  validateNewMeenSafe(p);
  validateNewMeenP243(world);
  validateSWE02Anatta(world);
  validateSWE02Act2Contracts(world);
  if(Boolean(p.flags?.piddockTradeAgreed)!==Boolean((p.piddockArmsAgreements||[]).length))throw Error('SWE02: agente sem contrato.');
  validateSWE02Palace(world);
  validateSWE02PalaceOperations(world);
  const node = LONG_ARM_OF_THE_HUTT.nodes[p.currentNodeId];
  return {
    available: true,
    active: p.status === 'ACTIVE',
    completed: p.status === 'COMPLETED',
    archived: p.status === 'ARCHIVED',
    progress: clone(p),
    node: playerNode(node, p),
    encounter: currentLongArmOfTheHuttEncounter(world)
  };
}

export function currentLongArmOfTheHuttScene(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID) return null;
  const node = LONG_ARM_OF_THE_HUTT.nodes[p.currentNodeId];
  if (!node) return null;
  return { locationId: node.locationId, title: node.title, narration: node.narration, visibleNpcIds: [...(node.visibleNpcIds || [])] };
}

const SWE02_SKILLS=Object.freeze({'Enganação':'engano','Charme':'carisma','Coerção':'coaccion','Percepção':'percepcion','Computadores':'computadoras','Astrogação':'astronavegacion','Mecânica':'mecanica'});
function sourceCheckPool(check, progress) {
  const difficulty=check.difficulty==='Fácil'?1:check.difficulty==='Média'?2:check.difficulty==='Difícil'?3:check.difficulty==='Formidável'?4:null;
  const isComms=progress.currentNodeId==='palace-communications';
  return {difficulty,challenge:0,setback:isComms?(progress.flags?.palaceAlarmRaised?2:1):0};
}
export function currentLongArmOfTheHuttChecks(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') return [];
  const node = LONG_ARM_OF_THE_HUTT.nodes[p.currentNodeId];
  if(['krayt-fang-logs','krayt-fang-ground-inspection'].includes(p.currentNodeId))return SHIP_CHECKS.map((check,index)=>({check,index})).filter(({index})=>shipCheckAvailable(p,index)).map(({check,index})=>({
    id:`${p.currentNodeId}:check:${index}`,index,skill:check.skill,
    skillKey:SWE02_SKILLS[check.skill]||null,difficultyText:['','Fácil','Média','Difícil','Formidável'][check.difficulty],purpose:check.purpose,
    pool:p.currentNodeId==='krayt-fang-ground-inspection'?{difficulty:check.difficulty,challenge:0,setback:0}:shipNoisePool(p,check.difficulty),shipOdorRule:check.key,source:{title:SOURCE.title,productCode:'SWE02',printedPage:check.page,authority:'OFFICIAL_PDF_TEXT_VERIFIED'},
    requiresGMSourceVerification:!SWE02_SKILLS[check.skill]
  }));
  if(p.currentNodeId==='palace-communications'&&p.flags?.palaceComputersLocked)return [];
  if(p.currentNodeId==='drombb-cantina'&&!p.flags?.drombbThreatNeutralized)return [];
  return (node?.checks||[]).map((check,index)=>({
    id:`${p.currentNodeId}:check:${index}`,index,skill:check.skill,
    skillKey:Number.isInteger(check.sourcePage)?(SWE02_SKILLS[check.skill]||null):null,
    difficultyText:check.difficulty,purpose:check.purpose,
    pool:sourceCheckPool(check,p),
    source:Number.isInteger(check.sourcePage)?{title:SOURCE.title,productCode:'SWE02',printedPage:check.sourcePage,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}:null,
    requiresGMSourceVerification:!Number.isInteger(check.sourcePage)
  }));
}

export function adjudicateLongArmOfTheHuttCheck(world, { checkIndex = 0, result = {} } = {}) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');
  const checks = currentLongArmOfTheHuttChecks(next);
  const check = checks.find(x=>x.index===Number(checkIndex));
  if (!check) throw new Error('Teste inválido.');
  if (typeof result.success !== 'boolean') throw new Error('Resultado de teste deve ser booleano.');
  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, success: result.success,
    source:check.source||null,netAdvantage:Number.isFinite(Number(result.netAdvantage))?Number(result.netAdvantage):null };
  p.flags={...(p.flags||{})};
  if(['krayt-fang-logs','krayt-fang-ground-inspection'].includes(p.currentNodeId))recordShipCheck(p,checkIndex,result.success);
  if(result.success){
    if(p.currentNodeId==='piddock-party'&&checkIndex===0)p.flags.geoPiddockCharmPassed=true;
    if(p.currentNodeId==='drombb-cantina'&&checkIndex===0)p.flags.geoDrombbCheckPassed=true;
    if(p.currentNodeId==='palace-front-gate'&&checkIndex===0)p.flags.palaceGateBluffPassed=true;
    if(p.currentNodeId==='palace-front-gate'&&checkIndex===1)p.flags.palaceDisguisePassed=true;
    if(p.currentNodeId==='palace-disguise-audience'&&checkIndex===0)p.flags.palaceAudiencePassed=true;
    if(p.currentNodeId==='palace-kitchen'&&checkIndex===0)p.flags.palaceKitchenFoundRyll=true;
  }
  if(p.currentNodeId==='palace-communications'){
    if(p.flags?.palaceComputersLocked)throw Error('Terminal bloqueado; novas consultas proibidas.');
    p.flags={...(p.flags||{})};
    if(result.success){
      if(checkIndex===1)p.flags.palaceAlarmsDisabled=true;
      if(checkIndex===2)p.flags.ryllFinancialEvidence=true;
      if(checkIndex===3)p.flags.b1ReverseEngineeringEvidence=true;
      if(checkIndex===4)p.flags.bithSpyEvidence=true;
    }
    if(Number(result.netAdvantage)<=-2)p.flags.palaceComputersLocked=true;
  }
  p.checkHistory.push(entry);
  p.updatedAt = now();
  next.publishedAdventure = p;
  return { world: next, progress: clone(p), adjudication: entry };
}

export function currentLongArmOfTheHuttMasterRequest(world) {
  const s = longArmOfTheHuttStatus(world);
  if (!s.active && !s.completed) return null;
  const node = s.node;
  if (!node) return null;
  return {
    kind: 'published_adventure',
    adventureId: ID,
    adventureTitle: LONG_ARM_OF_THE_HUTT.title,
    nodeId: node.id,
    episode: node.episode,
    locationId: node.locationId,
    summary: node.summary,
    checks: currentLongArmOfTheHuttChecks(world),
    suggestedActions: (node.choices || []).map(c => c.label),
    antiSpoiler: true,
    source: SOURCE
  };
}

export function currentLongArmOfTheHuttEncounter(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') return null;
  const history = p.encounterHistory || [];
  let id = null, label = null;
  if (p.currentNodeId === 'new-meen-night-raid') {
    id = 'laoth-new-meen-night-raid';
    label = 'Capangas de Drombb — ataque noturno a New Meen';
  }
  if (p.currentNodeId === 'drombb-cantina') {
    id = 'laoth-drombb-cantina';
    label = 'Angu Drombb e seus capangas — cantina';
  }
  if (p.currentNodeId === 'lylek-den-ambush') {
    id = 'lylek-den-bounty-hunters';
    label = 'Emboscada dos caçadores de Teemo no covil de lylek';
  }
  if (p.currentNodeId === 'gantry-aa7') {id='laoth-gantry-aa7';label='Gantry AA7 — soldados e Vrixx’tt';}
  if (p.currentNodeId === 'palace-gladiator-cells' && !p.flags?.palaceGladiatorsAllied) {
    id='laoth-palace-gladiators';label='Quatro gladiadores de Teemo';
  }
  if (p.currentNodeId === 'teemo-showdown') {
    id = 'teemo-palace-guards';
    label = 'Confronto com os Guardas de Teemo';
  }
  if (!id) return null;
  const resolved = history.some(x => x.encounterId === id && x.outcome);
  return { id, label, available: !resolved, status: resolved ? 'resolved' : (p.activeEncounterId === id ? 'active' : 'ready') };
}

export function longArmOfTheHuttEncounterSpec(world, encounterId = null) {
  const current = currentLongArmOfTheHuttEncounter(world);
  const id = String(encounterId || current?.id || '');
  if (['laoth-new-meen-night-raid','laoth-drombb-cantina'].includes(id)) {
    // SWE02 pp.19–22: 4+ PCs -> 3x4, 3 PCs -> 3x3, 2 PCs -> 3x2.
    // Solo or unknown party size has no published count: require GM review.
    const confirmed=Number(world?.publishedAdventure?.flags?.confirmedPartySize);
    const actual=Number.isInteger(confirmed)&&confirmed>=2&&confirmed<=8?Math.min(4,confirmed):null;
    const groupSize=actual===null?0:actual;
    const nightStopped=world?.publishedAdventure?.flags?.newMeenNightRaidStopped===true;
    const groups=id==='laoth-new-meen-night-raid'?1:(nightStopped?2:3);
    const bribed=id==='laoth-drombb-cantina'?remainingDrombbThugs(world?.publishedAdventure):0;
    let left=bribed;
    const list=Array.from({length:groups},(_,i)=>{
      const removed=Math.min(groupSize,left);left-=removed;
      return {profileKey:'laoth_drombb_thug',count:groupSize-removed,range:'Médio',
        name:`Capangas de Drombb — grupo ${i+1+(nightStopped?1:0)}`};
    }).filter(x=>x.count>0);
    return {
      id,
      label:id==='laoth-new-meen-night-raid'?'Ataque noturno — capangas de Drombb':'Cantina de Angu Drombb',
      source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPages:id==='laoth-new-meen-night-raid'?[20,21]:[21,22]},
      requiresPartySizeConfirmation:actual===null,
      confirmedPartySize:actual,
      environment:id==='laoth-new-meen-night-raid'?{darknessSetbackDice:1,requiresDiceIntegration:true}:null,
      playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
      adversaries:id==='laoth-drombb-cantina'?[...list,{profileKey:'laoth_angu_drombb',count:1,range:'Médio'}]:list
    };
  }
  if (id === 'lylek-den-bounty-hunters') {
    // SWE02 p.16: one hunter per PC, including one Gand with a blaster rifle.
    // If the group size is unavailable, return the solo baseline and request
    // GM confirmation rather than silently inventing a four-player party.
    const explicit = world?.publishedAdventure?.flags?.confirmedPartySize;
    const members = world?.party?.members ?? world?.group?.members;
    const derived = Array.isArray(members) ? members.filter(m=>m?.active!==false).length : null;
    const partySize = Number.isInteger(explicit)&&explicit>=1&&explicit<=8 ? explicit :
      Number.isInteger(derived)&&derived>=1&&derived<=8 ? derived : 1;
    return {
      id,label:'Caçadores de Teemo — antigo covil de lylek',
      source:{title:'Long Arm of the Hutt',productCode:'SWE02',page:16},
      partySize,requiresPartySizeConfirmation:!Number.isInteger(explicit)&&!Number.isInteger(derived),
      playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
      adversaries:[{profileKey:'laoth_gand_bounty_hunter',count:1,range:'Longo'},
        ...(partySize>1?[{profileKey:'laoth_bounty_hunter',count:partySize-1,range:'Médio'}]:[])]
    };
  }
  if(id==='laoth-gantry-aa7'){
    const p=world?.publishedAdventure;validateGeonosis(p);
    if(p?.currentNodeId!=='gantry-aa7'||!p.flags?.geoFinalized)throw Error('Gantry AA7 exige audiência pontuada.');
    return {id,label:'Gantry AA7',source:{productCode:'SWE02',printedPages:[36,37]},
      playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
      environment:{cargoCoverSetbackDice:1,requiresCoverTargeting:true},
      geonosianGuardCount:p.flags.geoGuards,
      adversaries:[...Array.from({length:p.flags.geoGuards},(_,i)=>({profileKey:'laoth_geonosian_soldier',count:1,range:'Médio',name:`Soldado AA7 ${i+1}`})),
        {profileKey:'laoth_vrixxtt_findsman',count:1,range:'Longo'}]};
  }
  if(id==='laoth-palace-gladiators'){
    if(world?.publishedAdventure?.currentNodeId!=='palace-gladiator-cells'||world.publishedAdventure.flags?.palaceGladiatorsAllied)throw Error('SWE02: gladiadores não hostis nesta cena.');
    return {id,label:'Quatro gladiadores de Teemo',source:{productCode:'SWE02',printedPages:[21,44]},
      playerInitiativeSkill:'frialdad',npcInitiativeSkill:'alerta',
      adversaries:[{profileKey:'laoth_drombb_thug',count:4,range:'Curto',name:'Quatro gladiadores'}]};
  }
  if (id === 'teemo-palace-guards') {
    // SWE02 printed pp.41,44–45: 2 elite guards beside throne, one group
    // of henchmen equal to the number of players. The second table group
    // has no fixed count in print: it is NOT silently invented.
    const explicit=Number(world?.publishedAdventure?.flags?.confirmedPartySize);
    const partySize=Number.isInteger(explicit)&&explicit>=2&&explicit<=8?explicit:null;
    const extra=Number(world?.publishedAdventure?.flags?.palaceAdditionalHenchmenCount);
    const extraConfirmed=Number.isInteger(extra)&&extra>=1&&extra<=8;
    const huttProfileConfirmed=world?.publishedAdventure?.flags?.gmHuttProfileConfirmed===true;
    return {
      id,label:'Trono de Teemo — dois guardas, capangas e Teemo',
      source:{title:SOURCE.title,productCode:'SWE02',printedPages:[41,44,45,46]},
      requiresPartySizeConfirmation:partySize===null,confirmedPartySize:partySize,
      requiresGMAdditionalGroupReview:!extraConfirmed,
      requiresOfficialHuttProfileReview:!huttProfileConfirmed,
      playerInitiativeSkill:'frialdad',npcInitiativeSkill:'alerta',
      adversaries:[
        {profileKey:'laoth_elite_gamorrean_guard',count:2,range:'Curto'},
        {profileKey:'laoth_drombb_thug',count:partySize||1,range:'Curto'},
        ...(extraConfirmed?[{profileKey:'laoth_drombb_thug',count:extra,range:'Curto',name:'Capangas da mesa do trono — grupo confirmado pelo Mestre'}]:[]),
        {profileKey:'teemo_the_hutt',worldNpcId:NPC.teemo,range:'Médio'}
      ]
    };
  }
  throw new Error('Nenhum encontro de combate de O Longo Braço do Hutt neste ponto.');
}

export function markLongArmOfTheHuttEncounterActive(world, encounterId) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não ativa.');
  const current = currentLongArmOfTheHuttEncounter(next);
  if (!current || current.id !== encounterId || !current.available || current.status !== 'ready')
    throw new Error('Encontro incompatível com a cena atual ou já resolvido.');
  p.activeEncounterId = encounterId;
  p.updatedAt = now();
  next.publishedAdventure = p;
  return next;
}

export function recordLongArmOfTheHuttEncounterOutcome(world, { encounterId, outcome, adversaries = [], throneAlarm=null } = {}) {
  const next = clone(world || {});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não ativa.');
  const current = currentLongArmOfTheHuttEncounter(next);
  if (!current || current.id !== encounterId || !current.available || p.activeEncounterId !== encounterId)
    throw new Error('Encontro incompatível, não iniciado ou já resolvido.');
  if (!['adversaries_defeated','player_incapacitated','ended','escaped','negotiated'].includes(outcome))
    throw new Error('Resultado de encontro inválido.');
  p.encounterHistory = Array.isArray(p.encounterHistory) ? p.encounterHistory : [];
  let confirmedCombat = null;
  if (encounterId==='laoth-new-meen-night-raid'||encounterId==='laoth-drombb-cantina') {
    const expected=longArmOfTheHuttEncounterSpec(next,encounterId).adversaries;
    // The controller supplies all enemy groups from its completed combat.
    // A manually forged 'victory' flag or result is never enough.
    if (!Array.isArray(adversaries) || adversaries.length!==expected.length ||
        expected.some((e,i)=>adversaries[i]?.profileKey!==e.profileKey))
      throw new Error('Fechamento de New Meen exige os grupos reais do combate.');
    if(adversaries.some(a=>typeof a.id!=='string'||!a.id.trim()) || new Set(adversaries.map(a=>a.id)).size!==adversaries.length)
      throw new Error('Grupos de New Meen sem identidades únicas de combate.');
    const surrendered=new Set((p.newMeenCoercionReceipts||[]).filter(r=>r.success).flatMap(r=>r.surrenderedGroupIds));
    const eliminated=adversaries.every(a=>a.status==='incapacitated'||a.status==='defeated'||(a.profileKey==='laoth_drombb_thug'&&a.status==='surrendered'&&surrendered.has(a.id)));
    if(adversaries.some(a=>a.status==='surrendered'&&!surrendered.has(a.id)))throw Error('New Meen: rendição sem teste real.');
    if (outcome==='adversaries_defeated'&&!eliminated)
      throw new Error('Vitória em New Meen sem adversários realmente incapacitados.');
    confirmedCombat={groupCount:expected.length,enemyIds:adversaries.map(a=>a.id),eliminated,
      surrenderedGroupIds:[...surrendered],groupSnapshots:adversaries.map(a=>({id:a.id,profileKey:a.profileKey,status:a.status,membersTotal:a.membersTotal,perMember:a.perMember,wounds:a.wounds}))};
  }
  if(encounterId==='laoth-gantry-aa7'){
    const expected=longArmOfTheHuttEncounterSpec(next,encounterId).adversaries;
    if(!Array.isArray(adversaries)||adversaries.length!==expected.length||expected.some((x,i)=>x.profileKey!==adversaries[i]?.profileKey))throw Error('Gantry: grupos do combate ausentes.');
    if(adversaries.some(x=>!x.id)||new Set(adversaries.map(x=>x.id)).size!==adversaries.length)throw Error('Gantry: IDs não únicos.');
    const bribedId=p.vrixxttBribeReceipt?.vrixxttId||null;
    const eliminated=adversaries.every(x=>['incapacitated','defeated'].includes(x.status)||
      (x.profileKey==='laoth_vrixxtt_findsman'&&x.id===bribedId&&x.status==='retreated'));
    if(outcome==='adversaries_defeated'&&!eliminated)throw Error('Gantry: vitória sem inimigos incapacitados ou suborno verificado.');
    if(outcome==='negotiated')throw Error('Gantry: suborno requer pagamento verificado; não implementado.');
    confirmedCombat={groupCount:expected.length,enemyIds:adversaries.map(x=>x.id),eliminated,bribedVrixxttId:bribedId};
  }
  if(encounterId==='laoth-palace-gladiators'){
    if(adversaries.length!==1||adversaries[0]?.profileKey!=='laoth_drombb_thug'||!adversaries[0]?.id)throw Error('SWE02: gladiadores sem grupo real.');
    if(outcome==='adversaries_defeated'&&!['incapacitated','defeated'].includes(adversaries[0].status))throw Error('SWE02: gladiadores não derrotados.');
    confirmedCombat={groupCount:1,enemyIds:[adversaries[0].id],eliminated:['incapacitated','defeated'].includes(adversaries[0].status)};
  }
  if(encounterId==='teemo-palace-guards'){
    const spec=longArmOfTheHuttEncounterSpec(next,encounterId);
    const expected=spec.adversaries;
    if(spec.requiresPartySizeConfirmation||spec.requiresGMAdditionalGroupReview||spec.requiresOfficialHuttProfileReview)
      throw Error('Teemo exige tamanho de grupo, segunda guarda e perfil do Hutt confirmados.');
    const base=adversaries.slice(0,expected.length);
    const reinforcements=adversaries.slice(expected.length);
    if(base.length!==expected.length||expected.some((e,i)=>base[i]?.profileKey!==e.profileKey)||reinforcements.length>1||reinforcements.some(x=>x.profileKey!=='gamorrean_guard'))
      throw Error('Teemo exige os grupos reais do combate, reforços confirmados e ficha do Hutt.');
    if(adversaries.some(a=>typeof a.id!=='string'||!a.id.trim())||new Set(adversaries.map(a=>a.id)).size!==adversaries.length)
      throw Error('Teemo exige identidades únicas de combate.');
    const chandelierConfirmed=(p.act3ChandelierReceipts||[]).some(x=>x.success===true&&x.encounterId==='teemo-palace-guards');
    if(reinforcements.length>0 && (!throneAlarm?.arrived||throneAlarm.enemyId!==reinforcements[0].id||throneAlarm.confirmedBarracksCount!==reinforcements[0].membersTotal||throneAlarm.arrivalRound<=throneAlarm.dueRound))
      throw Error('Teemo: reforços sem alarme, iniciativa e quantidade confirmados.');
    if(throneAlarm?.arrived && reinforcements.length!==1)throw Error('Teemo: reforços do quartel não constam do combate.');
    const eliminated=adversaries.every(a=>a.status==='incapacitated'||a.status==='defeated'||(chandelierConfirmed&&a.status==='retreated'&&a.profileKey!=='teemo_the_hutt'));
    const teemoIncapacitated=['incapacitated','defeated'].includes(adversaries.find(a=>a.profileKey==='teemo_the_hutt')?.status);
    if(outcome==='adversaries_defeated'&&(!eliminated||!teemoIncapacitated))
      throw Error('Teemo não foi derrotado; combate ou Hutt ainda ativo.');
    confirmedCombat={groupCount:adversaries.length,enemyIds:adversaries.map(a=>a.id),eliminated,teemoIncapacitated,reinforcementCount:reinforcements.length};
  }
  const record = { at: now(), nodeId: p.currentNodeId, encounterId, outcome, ...(confirmedCombat?{combatProof:confirmedCombat}:{}) };
  p.encounterHistory.push(record);
  p.activeEncounterId = null;
  p.flags = { ...(p.flags || {}) };
  if(encounterId==='laoth-new-meen-night-raid'){p.flags.newMeenNightRaidStopped=outcome==='adversaries_defeated';if(outcome==='player_incapacitated')p.flags.newMeenOutcome='LOST';}
  if(encounterId==='laoth-drombb-cantina'){p.flags.drombbThreatNeutralized=outcome==='adversaries_defeated';if(outcome==='player_incapacitated'){p.flags.drombbCaptured=true;p.flags.newMeenOutcome='LOST';}}
  if(encounterId==='teemo-palace-guards')p.flags.teemoDefeated=outcome==='adversaries_defeated';
  if(encounterId==='laoth-gantry-aa7'){p.flags.geoGantryCleared=['adversaries_defeated','escaped','player_incapacitated'].includes(outcome);p.flags.geoRescuedByVios=outcome==='player_incapacitated';}
  if(encounterId==='lylek-den-bounty-hunters')p.flags.lylekHuntersResolved=outcome==='adversaries_defeated'||outcome==='negotiated'||outcome==='escaped';
  p.updatedAt = now();
  next.publishedAdventure = p;
  return { world: next, record };
}

export function markLongArmOfTheHuttRewardsApplied(world, details = {}) {
  const next = clone(world || {});
  next.publishedAdventureRewards = { ...(next.publishedAdventureRewards || {}) };
  next.publishedAdventureRewards[ID] = {
    applied: true,
    appliedAt: now(),
    credits: Number.isFinite(Number(details.credits)) ? Number(details.credits) : 0,
    obligationReduced: Number.isFinite(Number(details.obligationReduced)) ? Number(details.obligationReduced) : 0,
    note: details.note || 'Desfecho contra Teemo: créditos e Obligation exigem adjudicação explícita'
  };
  if (next?.publishedAdventure?.id === ID) {
    next.publishedAdventure.rewardsApplied = true;
    next.publishedAdventure.updatedAt = now();
  }
  return next;
}

export function archiveLongArmOfTheHutt(world) {
  const next = clone(world || {});
  const p = next?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'COMPLETED') {
    throw new Error('O Longo Braço do Hutt precisa estar concluída antes de arquivar.');
  }
  p.status = 'ARCHIVED';
  p.archivedAt = now();
  p.updatedAt = now();
  next.publishedAdventure = p;
  return next;
}

export function longArmShipSmellSetback(world,characterName,checkIndex){
 const p=world?.publishedAdventure;return p?.id===ID?shipSmellSetback(p,characterName,SHIP_CHECKS[checkIndex]?.key):0;
}
