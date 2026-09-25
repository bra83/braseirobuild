import { CANONICAL_SKILLS } from './skillRegistry';
type Lifestyle = 'Frugal'|'Comum'|'Próspero';
type Proficiency = 'Machados'|'Arcos'|'Lanças'|'Espadas';
type SkillName = typeof CANONICAL_SKILLS[number];

export type SupplementSource = {
  id: string;
  title: string;
  authority: 'core-2e'|'supplement-2e'|'solo-2e';
  driveFileIds: string[];
};

export type SupplementCulture = {
  id: string;
  culture: string;
  aliases?: string[];
  lifestyle: Lifestyle;
  blessing: string;
  sourceId: string;
  attributes: Array<[number,number,number]>;
  derived: [number,number,number];
  skills: number[];
  culturalFavouredOptions: SkillName[];
  prof2: Proficiency[];
  features: string[];
  names: string[];
  ages: [number,number];
  usefulItems: number;
};

export type SupplementVirtue = {
  id: string;
  name: string;
  cultureIds: string[];
  sourceId: string;
  summary: string;
  tags: string[];
};

export type SupplementPatron = {
  id: string;
  name: string;
  sourceId: string;
  favouredCallings: string[];
  fellowshipBonus: number;
  agenda: string;
  benefit: string;
};

export const SUPPLEMENT_SOURCES: SupplementSource[] = [
  { id:'tor2e-core', title:'O Um Anel 2e — Livro Básico', authority:'core-2e', driveFileIds:['15umbJCMoLAortDPoicWeIHOzaREI9PBg','1mOdsOfqQASmLNqMZZIOz6aPatJHV1zhq','1M1rIXVz1L4Qzz98l5_aTF1c8sOokW4uA'] },
  { id:'tor2e-rivendell', title:'O Um Anel 2e — Valfenda', authority:'supplement-2e', driveFileIds:['15mAAieBwcvOfP9zqO54da-L-_-Cx9hFp'] },
  { id:'tor2e-wilderland', title:'O Um Anel 2e — Povos das Terras Ermas / Terras-Selváticas', authority:'supplement-2e', driveFileIds:['15wjw3LYORJeBvG2OB3w4SwbgBl6c-xto','15r-r6SFx3RWnRTlI7i-S4JGYp4zGYdH1'] },
  { id:'tor2e-strider', title:'O Um Anel 2e — Modo Peregrino', authority:'solo-2e', driveFileIds:['15gUwEnPRaD6f1FjXs0ho_j8VS5Rq5saY'] },
];

export const SUPPLEMENT_CULTURES: SupplementCulture[] = [
  {
    id:'high-elves-rivendell',
    culture:'Alto Elfo de Valfenda',
    aliases:['Alto Elfo','High Elf of Rivendell','High Elf'],
    lifestyle:'Próspero',
    blessing:'Elfo — se não estiver Arrasado, pode gastar 1 Esperança para obter um Sucesso Mágico em uma jogada de Perícia; durante a criação, aumente um Atributo em 1. Atingido pelo Desafio: Sombra acumulada só pode ser removida durante Yule.',
    sourceId:'tor2e-rivendell',
    attributes:[[5,2,7],[4,3,7],[5,3,6],[4,4,6],[5,4,5],[6,2,6]],
    derived:[22,6,12],
    skills:[2,0,0,2,1,0,2,0,0,0,2,0,2,1,0,2,2,3],
    culturalFavouredOptions:['Cura','História'],
    prof2:['Lanças','Espadas'],
    features:['Belo','Olhar Aguçado','Nobre','Inquisitivo','Alegre','Orgulhoso','Sutil','Teimoso'],
    names:['Aegnor','Beleg','Celegorm','Daeron','Edrahil','Fingon','Finrod','Gwindor','Mablung','Maeglin','Orodreth','Saeros','Amarië','Ancalimë','Celebrindal','Elwing','Finduilas','Fíriel','Idril','Míriel','Nimloth'],
    ages:[100,500],
    usefulItems:3,
  },
  {
    id:'beornings',
    culture:'Beorning',
    aliases:['Beornings'],
    lifestyle:'Comum',
    blessing:'Furioso — quando está Ferido, todas as suas jogadas de Ataque e de Proteção são Favorecidas.',
    sourceId:'tor2e-wilderland',
    attributes:[[5,7,2],[4,7,3],[5,6,3],[4,6,4],[5,5,4],[6,6,2]],
    derived:[22,6,12],
    skills:[3,1,0,2,1,1,2,3,1,2,1,0,0,0,1,1,1,0],
    culturalFavouredOptions:['Atletismo','Busca'],
    prof2:['Machados','Lanças'],
    features:['Bravo','Feroz','Generoso','Honrado','Rústico','Severo','Alto','Cauteloso'],
    names:['Agilfrid','Arnulf','Avagis','Baldac','Barald','Berangar','Eberulf','Gerold','Grimfast','Hartmut','Ramnulf','Theodard','Amalfrida','Avina','Beranhild','Brunihild','Ingund','Radegund'],
    ages:[14,45],
    usefulItems:2,
  },
  {
    id:'mirkwood-elves',
    culture:'Elfo da Floresta das Trevas',
    aliases:['Elfo Silvestre','Elfos da Floresta das Trevas','Mirkwood Elf','Wood-elf of Mirkwood'],
    lifestyle:'Frugal',
    blessing:'Povo Crepuscular — em uma floresta ou à noite, pode gastar 1 Esperança para obter um Sucesso Mágico em uma jogada de Perícia.',
    sourceId:'tor2e-wilderland',
    attributes:[[5,2,7],[4,3,7],[5,3,6],[4,4,6],[5,4,5],[6,2,6]],
    derived:[18,8,14],
    skills:[1,0,0,2,0,3,2,0,2,2,1,1,2,0,0,1,1,2],
    culturalFavouredOptions:['Furtividade','Música'],
    prof2:['Arcos','Lanças'],
    features:['Astuto','Belo','Feroz','Olhar Aguçado','Alegre','Orgulhoso','Sigiloso','Ágil'],
    names:['Amras','Aredhel','Beleganor','Belegon','Calanhir','Carmagor','Durandir','Edrahil','Galdor','Ithildir','Lascalan','Anórel','Aranel','Calanril','Elanor','Elwing','Idril','Nimloth','Niniel'],
    ages:[100,500],
    usefulItems:1,
  },
  {
    id:'woodmen-wilderland',
    culture:'Homem da Floresta das Terras Ermas',
    aliases:['Homem da Floresta','Homens da Floresta das Terras Ermas','Woodman of Wilderland','Woodmen'],
    lifestyle:'Frugal',
    blessing:'Andarilho da Floresta — adicione +2 ao Bloqueio ao lutar em uma floresta.',
    sourceId:'tor2e-wilderland',
    attributes:[[2,5,7],[3,4,7],[3,5,6],[4,4,6],[4,5,5],[2,6,6]],
    derived:[22,8,10],
    skills:[0,1,0,2,0,2,3,0,0,3,2,2,0,1,1,1,1,0],
    culturalFavouredOptions:['Cura','Caçada'],
    prof2:['Machados','Arcos'],
    features:['Astuto','Ansioso','Fiel','Paciente','Severo','Ágil','Coração Verdadeiro','Cauteloso'],
    names:['Amalric','Ansegisel','Audovald','Balderic','Beranald','Beormud','Euric','Grimbald','Hartgard','Reginar','Theodemir','Adosinda','Amalfrida','Basina','Beranhild','Gailavira','Ingund','Wisigard'],
    ages:[16,50],
    usefulItems:1,
  },
];

export const SUPPLEMENT_VIRTUES: SupplementVirtue[] = [
  { id:'riv-artifice-eregion', name:'Artífice de Eregion', cultureIds:['high-elves-rivendell'], sourceId:'tor2e-rivendell', tags:['fellowship','yule','magic-item'], summary:'No próximo Yule, pode adicionar uma Recompensa Encantada élfica a uma arma ou criar um Artefato Maravilhoso. Em uma Fase de Sociedade, um teste bem-sucedido de Ofício ou História revela as qualidades de um Artefato/Item Maravilhoso.' },
  { id:'riv-beauty-stars', name:'Beleza das Estrelas', cultureIds:['high-elves-rivendell'], sourceId:'tor2e-rivendell', tags:['hope-max','council','eye'], summary:'Esperança máxima +1. Como porta-voz em um Conselho, pode aumentar o limite de tempo em 1 ou fazer os mortais presentes reterem apenas uma lembrança vaga; esta segunda opção conta como magia menor para a Vigilância do Olho.' },
  { id:'riv-firstborn-power', name:'Poder do Primogênito', cultureIds:['high-elves-rivendell'], sourceId:'tor2e-rivendell', tags:['combat','hope','deadly'], summary:'Quando um adversário gasta Ódio ou Determinação para ativar uma Habilidade Mortal, pode gastar 1 Esperança para cancelar o efeito.' },
  { id:'riv-eldar-skill', name:'Habilidade dos Eldar', cultureIds:['high-elves-rivendell'], sourceId:'tor2e-rivendell', tags:['magical-success','feat-die'], summary:'Quando o Dado de Façanha obtém a runa de Gandalf, o resultado conta como Sucesso Mágico sem gastar Esperança.' },

  { id:'beorn-enchantment', name:'O Encantamento de Beorn', cultureIds:['beornings'], sourceId:'tor2e-wilderland', tags:['hope','magical-success','strength-skills','choice'], summary:'Escolha três Perícias de Força. Ao usar uma delas, pode gastar 1 Esperança para obter um Sucesso Mágico.' },
  { id:'beorn-bear-brother', name:'Irmão para os Ursos', cultureIds:['beornings'], sourceId:'tor2e-wilderland', tags:['endurance-max','brawl','bears'], summary:'Resistência máxima +1; melhora ataques desarmados e permite comunicar-se com ursos usando Perícias apropriadas.' },
  { id:'beorn-great-strength', name:'Grande Força', cultureIds:['beornings'], sourceId:'tor2e-wilderland', tags:['combat','heavy-blow'], summary:'Ao infligir Golpe Pesado como Dano Especial, some +2 ao nível de Força usado pelo efeito.' },
  { id:'beorn-hide-coat', name:'Casaco de Pele', cultureIds:['beornings'], sourceId:'tor2e-wilderland', tags:['protection','bonus-die'], summary:'Usando armadura de couro ou nenhuma armadura, ganha +1d em testes de Proteção.' },
  { id:'beorn-breaking-blow', name:'Golpe Ruptor', cultureIds:['beornings'], sourceId:'tor2e-wilderland', tags:['combat','piercing','protection'], summary:'Quando consegue um Golpe Perfurante em ataque corpo a corpo, a jogada de Proteção do alvo é Desfavorecida.' },
  { id:'beorn-honey-cakes', name:'Bolos de Mel Duas Vezes Assados', cultureIds:['beornings'], sourceId:'tor2e-wilderland', tags:['journey','fatigue','fellowship'], summary:'Sempre que ganha Fadiga em um Evento de Jornada, ganha 1 ponto a menos. A Sociedade da Companhia aumenta em 1.' },

  { id:'mirk-unseen', name:'Contra o Invisível', cultureIds:['mirkwood-elves'], sourceId:'tor2e-wilderland', tags:['shadow-test','dread','spirit'], summary:'Testes de Sombra por Pavor são Favorecidos; ganha +1d quando o teste é imposto por espírito maligno, fantasma ou criatura possuída.' },
  { id:'mirk-deadly-archery', name:'Arquearia Mortal', cultureIds:['mirkwood-elves'], sourceId:'tor2e-wilderland', tags:['combat','bow','rearward'], summary:'Com Arco (não Arco Grande) em Postura Retaguarda, pode usar Preparar Tiro como ação secundária.' },
  { id:'mirk-elven-lights', name:'Luzes Élficas', cultureIds:['mirkwood-elves'], sourceId:'tor2e-wilderland', tags:['hope','magic','combat'], summary:'Pode gastar 1 Esperança para acender uma luz mágica e atrair criaturas cujo Poder total não exceda Sabedoria; inimigos surpreendidos não fazem saraivadas de abertura nem agem na primeira rodada de combate próximo e ficam Cansados nas duas primeiras rodadas.' },
  { id:'mirk-elven-dreams', name:'Sonhos Élficos', cultureIds:['mirkwood-elves'], sourceId:'tor2e-wilderland', tags:['rest'], summary:'Não precisa dormir se puder realizar atividades simples e repetitivas. Um Descanso Curto conta como Descanso Prolongado.' },
  { id:'mirk-gleam-wrath', name:'Luzir de Ira', cultureIds:['mirkwood-elves'], sourceId:'tor2e-wilderland', tags:['combat','hate'], summary:'Em um ataque bem-sucedido, o adversário também perde 1 Ódio/Determinação, mais 1 por ícone de Sucesso.' },
  { id:'mirk-shots-dark', name:'Tiros no Escuro', cultureIds:['mirkwood-elves'], sourceId:'tor2e-wilderland', tags:['combat','inspired','night','forest'], summary:'Em uma floresta ou à noite, fica Inspirado em todas as jogadas de combate.' },

  { id:'wood-hunters-resolve', name:'A Resolução do Caçador', cultureIds:['woodmen-wilderland'], sourceId:'tor2e-wilderland', tags:['hope','heal-endurance'], summary:'Se não estiver Ferido nem Arrasado, pode gastar 1 Esperança para recuperar Resistência igual ao maior entre Coração e Valor.' },
  { id:'wood-herbal-remedies', name:'Remédios Herbais', cultureIds:['woodmen-wilderland'], sourceId:'tor2e-wilderland', tags:['healing','journey','fatigue'], summary:'Ganha +1d em testes de Cura. No fim da Jornada, pode fazer um teste de Cura para reduzir Fadiga acumulada além do teste de Viagem.' },
  { id:'wood-forest-exploration', name:'Exploração Florestal', cultureIds:['woodmen-wilderland'], sourceId:'tor2e-wilderland', tags:['combat','opening-volley','first-round'], summary:'Saraivadas de abertura e ataques corpo a corpo feitos na primeira rodada do combate são Favorecidos.' },
  { id:'wood-mirkwood-hound', name:'Cão da Floresta das Trevas', cultureIds:['woodmen-wilderland'], sourceId:'tor2e-wilderland', tags:['companion','awareness','hunting','combat'], summary:'Com o cão, ganha +1d em Vigilância ou Caçada. Em combate ele conta como outro herói em postura corpo a corpo; o primeiro ataque corpo a corpo contra você a cada rodada perde 1d. Pode cancelar um Golpe Perfurante ferindo o cão.' },
  { id:'wood-natural-watchfulness', name:'Vigilância Natural', cultureIds:['woodmen-wilderland'], sourceId:'tor2e-wilderland', tags:['hope','magical-success','journey-role'], summary:'Em Vigilância, Exploração ou Busca, pode gastar 1 Esperança para obter Sucesso Mágico. Pode cobrir Vigia além de outro papel de Jornada.' },
  { id:'wood-staunching-song', name:'Música de Estancamento', cultureIds:['woodmen-wilderland'], sourceId:'tor2e-wilderland', tags:['hope-max','first-aid','song'], summary:'Esperança máxima +1. Pode testar Música para Primeiros Socorros ou para salvar heróis Morrendo no lugar de Cura.' },
];

export const SUPPLEMENT_PATRONS: SupplementPatron[] = [
  {
    id:'elrond-half-elven',
    name:'Elrond Meio-Elfo',
    sourceId:'tor2e-rivendell',
    favouredCallings:['Estudioso','Guardião'],
    fellowshipBonus:1,
    agenda:'Proteger a terra e preservar o legado das eras.',
    benefit:'Ao Encontrar Patrono para visitar Elrond, a Companhia recebe +1 nas rolagens de Façanha usadas para determinar Eventos de Jornada e aprende as qualidades de todos os Artefatos Maravilhosos e Itens Maravilhosos em sua posse.',
  },
];

export const STRIDER_MODE_RULES = {
  sourceId:'tor2e-strider',
  previousExperience:15,
  baseFellowship:3,
  noFellowshipFocus:true,
  noJourneyRoles:true,
  distinctiveFeature:'Peregrino',
  journeyInspired:true,
  combatStance:'skirmish',
} as const;

function norm(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').trim();
}

export function supplementCultureFor(value: string): SupplementCulture | undefined {
  const key=norm(value);
  return SUPPLEMENT_CULTURES.find(culture => [culture.culture,...(culture.aliases||[])].some(name => norm(name)===key));
}

export function sourceForCulture(value: string): string {
  return supplementCultureFor(value)?.sourceId || 'tor2e-core';
}

export function virtuesForCulture(value: string): SupplementVirtue[] {
  const culture=supplementCultureFor(value);
  if(!culture) return [];
  const inherited = culture.id==='high-elves-rivendell'
    ? SUPPLEMENT_VIRTUES.filter(item => item.cultureIds.includes('high-elves-rivendell'))
    : SUPPLEMENT_VIRTUES.filter(item => item.cultureIds.includes(culture.id));
  return inherited;
}

export function supplementVirtueByName(value: string): SupplementVirtue | undefined {
  const key=norm(value);
  return SUPPLEMENT_VIRTUES.find(item => norm(item.name)===key);
}

export function supplementPatronByName(value: string): SupplementPatron | undefined {
  const key=norm(value);
  return SUPPLEMENT_PATRONS.find(item => norm(item.name)===key || (item.id==='elrond-half-elven' && key==='elrond'));
}
