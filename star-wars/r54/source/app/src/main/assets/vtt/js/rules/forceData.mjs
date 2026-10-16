export const FORCE_DIE_FACES = Object.freeze([
  Object.freeze({light:0,dark:1}), Object.freeze({light:0,dark:1}), Object.freeze({light:0,dark:1}),
  Object.freeze({light:0,dark:1}), Object.freeze({light:0,dark:1}), Object.freeze({light:0,dark:1}),
  Object.freeze({light:0,dark:2}),
  Object.freeze({light:1,dark:0}), Object.freeze({light:1,dark:0}),
  Object.freeze({light:2,dark:0}), Object.freeze({light:2,dark:0}), Object.freeze({light:2,dark:0})
]);

const source=(printedPage,section)=>Object.freeze({book:'Star Wars: Edge of the Empire — Core Rulebook',printedPage,section});
const talent=(row,column,originalName,cost,name,{forceOnly=false}={})=>Object.freeze({
  row,column,originalName,cost,id:`universal:force_sensitive_exile:r${row}c${column}`,name,forceOnly,source:source(277,'Force Sensitive Exile Talent Tree')
});
const exileNodes=[
  talent(1,1,'UNCSEN',5,'Sentidos sobrenaturais',{forceOnly:true}), talent(1,2,'INSIGHT',5,'Perspicácia'), talent(1,3,'FORAGER',5,'Forrageador'), talent(1,4,'UNCREA',5,'Reações sobrenaturais',{forceOnly:true}),
  talent(2,1,'CONV',10,'Atitude convincente'), talent(2,2,'OVEREMO',10,'Sobrecarregar emoções',{forceOnly:true}), talent(2,3,'INTFOC',10,'Foco intenso'), talent(2,4,'QUICKDR',10,'Saque rápido'),
  talent(3,1,'SENSEDANGER',15,'Sentir o perigo',{forceOnly:true}), talent(3,2,'SENSEEMO',15,'Sentir emoções',{forceOnly:true}), talent(3,3,'BALANCE',15,'Equilíbrio',{forceOnly:true}), talent(3,4,'TOUCHFATE',15,'Toque do destino',{forceOnly:true}),
  talent(4,1,'STRSMART',20,'Instinto das ruas'), talent(4,2,'UNCSEN',20,'Sentidos sobrenaturais',{forceOnly:true}), talent(4,3,'UNCREA',20,'Reações sobrenaturais',{forceOnly:true}), talent(4,4,'STRSMART',20,'Instinto das ruas'),
  talent(5,1,'SIXTHSENSE',25,'Sexto sentido',{forceOnly:true}), talent(5,2,'FORCE_RATING',25,'Afinidade com a Força',{forceOnly:true}), talent(5,3,'DEDI',25,'Dedicação'), talent(5,4,'SUPREF',25,'Reflexos superiores')
];
const e=(a,b)=>[`universal:force_sensitive_exile:${a}`,`universal:force_sensitive_exile:${b}`];
export const FORCE_SENSITIVE_EXILE_TREE=Object.freeze({
  nodes:Object.freeze(exileNodes),
  edges:Object.freeze([
    e('r1c1','r2c1'),e('r1c4','r2c4'),
    e('r2c1','r3c1'),e('r2c2','r3c2'),e('r2c3','r3c3'),e('r2c4','r3c4'),
    e('r3c1','r4c1'),e('r3c2','r4c2'),e('r3c3','r4c3'),e('r3c4','r4c4'),
    e('r4c1','r4c2'),e('r4c2','r4c3'),e('r4c3','r4c4'),
    e('r4c1','r5c1'),e('r4c2','r5c2'),e('r4c3','r5c3'),e('r4c4','r5c4'),
    e('r5c1','r5c2'),e('r5c2','r5c3'),e('r5c3','r5c4')
  ])
});

export const EOTE_UNIVERSAL_SPECIALIZATIONS=Object.freeze({
  force_sensitive_exile:Object.freeze({
    label:'Exilado Sensível à Força', skills:Object.freeze([]), universal:true, forceSensitive:true,
    source:source(276,'Becoming Force Sensitive'), talentTree:FORCE_SENSITIVE_EXILE_TREE
  })
});

function upgrade(id,cost,type,label,parents=[]){return Object.freeze({id,cost,type,label,parents:Object.freeze(parents),source:source(null,'Force Power Tree')});}
export const FORCE_POWER_TREES=Object.freeze({
  sense:Object.freeze({
    key:'sense',label:'Sentir',baseCost:10,source:source(279,'Force Power: Sense'),
    upgrades:Object.freeze([
      upgrade('control_defense',10,'control','Comprometer 1 dado da Força; uma vez por rodada, aprimora a dificuldade de um ataque recebido.',[]),
      upgrade('duration',10,'duration','O efeito contínuo pode ser acionado uma vez adicional por rodada.',['control_defense']),
      upgrade('strength',10,'strength','Efeitos contínuos aprimoram duas vezes em vez de uma.',['duration']),
      upgrade('control_offense',10,'control','Comprometer 1 dado da Força; uma vez por rodada, aprimora a habilidade de um teste de combate.',['strength']),
      upgrade('control_thoughts',10,'control','Gaste 1 ponto da Força para sentir os pensamentos atuais de um alvo vivo em alcance engajado.',[]),
      upgrade('range_1',5,'range','Aumenta o alcance por número de aprimoramentos de Alcance.',['control_thoughts']),
      upgrade('range_2',10,'range','Segundo aprimoramento de Alcance.',['range_1']),
      upgrade('range_3',10,'range','Terceiro aprimoramento de Alcance.',['range_2']),
      upgrade('magnitude_1',5,'magnitude','Aumenta o número de alvos pelo total de aprimoramentos de Magnitude.',['control_thoughts']),
      upgrade('magnitude_2',10,'magnitude','Segundo aprimoramento de Magnitude.',['magnitude_1']),
      upgrade('magnitude_3',10,'magnitude','Terceiro aprimoramento de Magnitude.',['magnitude_2'])
    ])
  }),
  influence:Object.freeze({
    key:'influence',label:'Influenciar',baseCost:10,source:source(281,'Force Power: Influence'),
    upgrades:Object.freeze([
      upgrade('range_1',5,'range','Primeiro aprimoramento de Alcance.',[]),
      upgrade('magnitude_1',5,'magnitude','Primeiro aprimoramento de Magnitude.',[]),
      upgrade('control_mind',10,'control','Teste oposto Disciplina vs. Disciplina combinado com Influência para impor emoção ou crença por 1 rodada/5 minutos.',[]),
      upgrade('control_social',15,'control','Em Coerção, Carisma, Enganação, Liderança ou Negociação, pode combinar Influência e gastar pontos para obter sucesso ou vantagem.',['range_1','magnitude_1']),
      upgrade('strength',10,'strength','O efeito básico causa 2 de Tensão por ponto gasto em vez de 1.',['control_mind']),
      upgrade('range_2',10,'range','Segundo aprimoramento de Alcance.',['control_social']),
      upgrade('range_3',10,'range','Terceiro aprimoramento de Alcance.',['range_2']),
      upgrade('magnitude_2',5,'magnitude','Segundo aprimoramento de Magnitude.',['control_social']),
      upgrade('magnitude_3',10,'magnitude','Terceiro aprimoramento de Magnitude.',['magnitude_2']),
      upgrade('duration_1a',5,'duration','Primeiro aprimoramento de Duração.',['strength']),
      upgrade('duration_1b',5,'duration','Aprimoramento de Duração conectado ao ramo mental.',['duration_1a']),
      upgrade('duration_2a',5,'duration','Aprimoramento adicional de Duração.',['duration_1a','duration_2b']),
      upgrade('duration_2b',5,'duration','Aprimoramento adicional de Duração.',['duration_1b','duration_2a'])
    ])
  }),
  move:Object.freeze({
    key:'move',label:'Mover',baseCost:10,source:source(283,'Force Power: Move'),
    upgrades:Object.freeze([
      upgrade('magnitude_1',5,'magnitude','Primeiro aprimoramento de Magnitude.',[]),
      upgrade('magnitude_2',5,'magnitude','Segundo aprimoramento de Magnitude.',['magnitude_1']),
      upgrade('magnitude_3',10,'magnitude','Terceiro aprimoramento de Magnitude.',['magnitude_2']),
      upgrade('magnitude_4',10,'magnitude','Quarto aprimoramento de Magnitude.',['magnitude_3']),
      upgrade('strength_1',10,'strength','Primeiro aprimoramento de Força.',[]),
      upgrade('strength_2',10,'strength','Segundo aprimoramento de Força.',['strength_1']),
      upgrade('strength_3',15,'strength','Terceiro aprimoramento de Força.',['strength_2']),
      upgrade('strength_4',20,'strength','Quarto aprimoramento de Força.',['strength_3']),
      upgrade('range_1a',5,'range','Primeiro aprimoramento de Alcance.',[]),
      upgrade('range_1b',5,'range','Primeiro aprimoramento de Alcance alternativo.',[]),
      upgrade('control_hurl',10,'control','Arremessa objetos com Disciplina + Mover; silhueta 0 causa 5 dano, demais 10 × silhueta.',['range_1a','range_1b']),
      upgrade('control_pull',5,'control','Pode arrancar objetos de suportes seguros ou da mão de um oponente.',['control_hurl']),
      upgrade('range_2',15,'range','Aprimoramento adicional de Alcance.',['control_hurl']),
      upgrade('control_fine',15,'control','Pode manipular objetos finamente como faria com as mãos.',['control_pull'])
    ])
  })
});
