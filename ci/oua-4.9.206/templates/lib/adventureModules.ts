import type { AdventureModule, CampaignStartMode, AdventureLocationDefinition } from './adventureTypes';

export const STAR_OF_MIST: AdventureModule = {
  id:'core-star-of-the-mist',
  title:'A Estrela da Névoa',
  campaignTitle:'Livro Básico · A Estrela da Névoa',
  source:{sourceBook:'O Um Anel 2e — Livro Básico',printedPages:'223+',driveFileId:'1M1rIXVz1L4Qzz98l5_aTF1c8sOokW4uA'},
  start:{
    canonicalName:'A Estrela da Névoa · sul de Ered Luin',
    aliases:['Ered Luin','Montanhas Azuis','Blue Mountains'],
    regionAliases:['Ered Luin','Montanhas Azuis','Blue Mountains'],
    bias:'southeast',
  },
  openingFacts:[
    'A Companhia começa nas proximidades do marco A Estrela da Névoa, no sul de Ered Luin.',
    'O marco envolve ruínas de uma antiga fortificação de Arnor construída sobre estruturas anãs mais antigas.',
    'Uma estranha luz azul foi vista na região e viajantes começaram a desaparecer nas proximidades.',
    'A abertura apresenta o lugar sem antecipar segredos ou decidir ações pelos jogadores.',
  ],
  gmFacts:['Segredos e causas do marco permanecem selados até que o estado da aventura permita revelá-los.'],
};

export const LONE_LANDS_CAMPAIGN: AdventureModule = {
  id:'lone-lands-troll-hole',
  title:'Toca de Troll, Se é Que Houve Uma…',
  campaignTitle:'Contos das Terras Solitárias · campanha de seis aventuras',
  source:{sourceBook:'Contos das Terras Solitárias',printedPages:'4–113',driveFileId:'15Zq2VPLjH6ys87LlHcH4e1yVakDcX6K-'},
  start:{
    canonicalName:'Bri',
    venueName:'Pônei Saltitante',
    aliases:['Bri','Bree','Pônei Saltitante','Ponei Saltitante','Prancing Pony'],
    regionAliases:['Terra de Bri','Bree-land'],
    bias:'none',
  },
  openingFacts:[
    'A campanha começa pela primeira aventura do volume.',
    'A primeira parte se passa no Pônei Saltitante, em Bri.',
    'Se esta for a primeira sessão, a Companhia pode estar se reunindo ali pela primeira vez.',
    'Jari e Diarmoc pertencem à situação inicial, mas seus segredos não são conhecimento inicial dos jogadores.',
  ],
  gmFacts:['A ordem das aventuras é sugerida; o Adventure Engine preserva agência e não força roteiro linear.'],
  campaignSequence:[
    'Toca de Troll, Se é Que Houve Uma…',
    'Mexendo com Barcos',
    'Reis de Pequenos Reinos',
    'Não Atacar sem Necessidade',
    'Maravilha do Norte do Mundo',
    'A Busca de Amon Guruthos',
  ],
};

export const ADVENTURE_MODULES: Record<string,AdventureModule> = {
  [STAR_OF_MIST.id]:STAR_OF_MIST,
  [LONE_LANDS_CAMPAIGN.id]:LONE_LANDS_CAMPAIGN,
};

export const STRIDER_PATRONS: Record<string,{id:string;label:string;start:AdventureLocationDefinition}> = {
  balin:{id:'balin',label:'Balin, Filho de Fundin',start:{canonicalName:'Montanhas Azuis',aliases:['Montanhas Azuis','Ered Luin','Blue Mountains'],regionAliases:['Ered Luin'],bias:'none'}},
  bilbo:{id:'bilbo',label:'Bilbo Bolseiro',start:{canonicalName:'Vila dos Hobbits',venueName:'Bolsão',aliases:['Vila dos Hobbits','Hobbiton','Condado','Shire'],regionAliases:['Condado','Shire'],bias:'none'}},
  cirdan:{id:'cirdan',label:'Círdan, o Marinheiro',start:{canonicalName:'Mithlond',venueName:'Portos Cinzentos',aliases:['Mithlond','Portos Cinzentos','Grey Havens','Lindon'],regionAliases:['Lindon'],bias:'none'}},
  gandalf:{id:'gandalf',label:'Gandalf, o Cinzento',start:{canonicalName:'Bri',venueName:'uma pousada de Bri',aliases:['Bri','Bree'],regionAliases:['Terra de Bri'],bias:'none'}},
  gilraen:{id:'gilraen',label:'Gilraen',start:{canonicalName:'Valfenda',aliases:['Valfenda','Rivendell','Imladris'],regionAliases:['Valfenda'],bias:'none'}},
  tom:{id:'tom',label:"Tom Bombadil e Senhora Fruta D'ouro",start:{canonicalName:'Casa de Tom Bombadil',aliases:['Casa de Tom Bombadil','Floresta Velha','Old Forest','Voltavime','Withywindle'],regionAliases:['Floresta Velha'],bias:'none'}},
};

export const CAMPAIGN_START_OPTIONS: Array<{mode:CampaignStartMode;label:string;description:string;recommended?:boolean}> = [
  {mode:'star-of-mist',label:'Livro Básico · A Estrela da Névoa',description:'Marco do livro básico. O Atlas posiciona a Companhia automaticamente na região do marco.',recommended:true},
  {mode:'lone-lands',label:'Contos das Terras Solitárias',description:'Campanha de seis aventuras. Começa em Bri, no Pônei Saltitante.'},
  {mode:'strider',label:'Modo Peregrino · Missão do Patrono',description:'Usa o Patrono do herói e uma semente de missão das tabelas do Modo Peregrino.'},
  {mode:'sandbox',label:'Sandbox · Eu escolho no Atlas',description:'Único modo que exige escolher manualmente o hex inicial.'},
  {mode:'tavern',label:'Taverna · Local aleatório',description:'Escolhe uma taverna mapeada ou uma taverna genérica em um assentamento do Atlas.'},
];
