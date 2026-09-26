import { secureRoll } from '../dice';

export type SkillChallengeDifficulty='simple'|'arduous'|'challenging';
export type SkillChallengeTime='short'|'enough'|'plenty'|'none';
export type SkillChallengeRisk='standard'|'dangerous'|'foolhardy';
export type EnduranceLossLevel='moderate'|'severe'|'grave';

export const SKILL_CHALLENGE_RESISTANCE:Record<SkillChallengeDifficulty,3|6|9>={
  simple:3,arduous:6,challenging:9,
};

export function skillChallengeTimeLimit(resistance:number,time:SkillChallengeTime):number|null{
  const value=Math.max(1,Math.trunc(Number(resistance)||1));
  if(time==='none')return null;
  if(time==='short')return value;
  if(time==='enough')return value+1;
  return value+2;
}

function featRankForHero(value:number):number{
  if(value===12)return 100;
  if(value===11)return -100;
  return -Math.max(1,Math.min(10,value));
}

export function rollEnduranceLoss(level:EnduranceLossLevel):{feat:number;loss:number;knockedOut:boolean;unharmed:boolean}{
  const rolls=level==='severe'?[secureRoll(1,12)]:[secureRoll(1,12),secureRoll(1,12)];
  let feat=rolls[0];
  if(level==='moderate')feat=[...rolls].sort((a,b)=>featRankForHero(b)-featRankForHero(a))[0];
  if(level==='grave')feat=[...rolls].sort((a,b)=>featRankForHero(a)-featRankForHero(b))[0];
  if(feat===12)return {feat,loss:0,knockedOut:false,unharmed:true};
  if(feat===11)return {feat,loss:0,knockedOut:true,unharmed:false};
  return {feat,loss:Math.max(1,Math.min(10,feat)),knockedOut:false,unharmed:false};
}

export type MountQuality='poor'|'common'|'prosperous'|'rich';

export function mountVigourForLifestyle(lifestyle:string):0|1|2|3{
  const key=String(lifestyle||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
  if(/muito rico|very rich|\brico\b|\brich\b/.test(key))return 3;
  if(/prospero|prosperous/.test(key))return 2;
  if(/comum|common/.test(key))return 1;
  return 0;
}

export function mountDescription(vigour:number):string{
  if(vigour>=3)return 'bela criatura';
  if(vigour===2)return 'criatura decente';
  if(vigour===1)return 'cavalo velho ou pônei mal alimentado';
  return 'sem montaria';
}

export interface PatronProfile{
  id:'balin'|'bilbo'|'cirdan'|'gandalf'|'gilraen'|'tom-goldberry'|'elrond'|'other';
  label:string;
  fellowshipBonus:number;
}
function patronKey(value:string):string{
  return String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
}
export function patronProfile(value:string):PatronProfile{
  const key=patronKey(value);
  if(key.includes('balin'))return {id:'balin',label:'Balin, filho de Fundin',fellowshipBonus:1};
  if(key.includes('bilbo'))return {id:'bilbo',label:'Bilbo Bolseiro',fellowshipBonus:2};
  if(key.includes('cirdan'))return {id:'cirdan',label:'Círdan, o Marinheiro',fellowshipBonus:1};
  if(key.includes('gandalf'))return {id:'gandalf',label:'Gandalf, o Cinzento',fellowshipBonus:2};
  if(key.includes('gilraen'))return {id:'gilraen',label:'Gilraen, filha de Dírhael',fellowshipBonus:0};
  if(key.includes('bombadil')||key.includes('fruta d')||key.includes('goldberry'))return {id:'tom-goldberry',label:"Tom Bombadil e Senhora Fruta d'Ouro",fellowshipBonus:2};
  if(key.includes('elrond'))return {id:'elrond',label:'Elrond Meio-Elfo',fellowshipBonus:0};
  return {id:'other',label:value||'Sem Patrono',fellowshipBonus:0};
}

export type PatronMissionPatron='balin'|'bilbo'|'cirdan'|'gandalf'|'gilraen'|'tom-goldberry';
export const PATRON_MISSIONS:Record<PatronMissionPatron,string[]>={
  balin:[
    'Investigar os sussurros sombrios que atormentam território anão e descobrir a sombra que espreita nos ermos.',
    'Encontrar um artefato quebrado e quem possua habilidade para reforjá-lo como arma contra o Inimigo.',
    'Explorar ruínas anãs em busca de segredos e armas contra o Inimigo.',
    'Recuperar o que um poderoso camarada caído carregava de vital para a sobrevivência de seu povo.',
    'Retomar ou investigar uma fortaleza anã tomada pelo Inimigo e descobrir quem a lidera.',
    'Descobrir como um poderoso tenente do Inimigo acumulou seguidores e impedir sua ascensão.',
  ],
  bilbo:[
    'Encontrar uma iguaria rara e descobrir onde ela pode ser obtida.',
    'Explorar uma área e registrar marcos significativos para atualizar a cartografia.',
    'Levar uma mensagem vital por estradas que se tornaram escuras e perigosas.',
    'Decifrar um texto antigo encontrando quem possui o conhecimento necessário.',
    'Recuperar um mapa de significado especial que caiu nas mãos do Inimigo.',
    'Encontrar um artefato importante antes que o Inimigo o faça.',
  ],
  cirdan:[
    'Entregar um recurso vital para a construção de navios enquanto os suprimentos diminuem.',
    'Investigar o retorno de um antigo inimigo dos Elfos e o rancor que ele guarda.',
    'Reacender um farol remoto e enfrentar a presença sombria que tenta impedir isso.',
    'Descobrir o destino de mensageiros cujas cartas caíram nas mãos do Inimigo.',
    'Recuperar um artefato élfico carregado por um servo do Inimigo.',
    'Descobrir e curar a doença que mancha as árvores sagradas usadas nos navios dos Portos Cinzentos.',
  ],
  gandalf:[
    'Levar boas notícias e esperança a um assentamento remoto.',
    'Levar notícias dos movimentos do Inimigo a um aliado de confiança.',
    'Ganhar a confiança de um possível aliado que se revelou.',
    'Capturar um servo do Inimigo e descobrir o que ele sabe.',
    'Descobrir uma arma terrível que o Inimigo pretende usar e quem a empunha.',
    'Resgatar um aliado capturado pela Sombra.',
  ],
  gilraen:[
    'Descobrir o que aconteceu com um Patrulheiro desaparecido.',
    'Investigar um poderoso servo do Inimigo que ameaça os Patrulheiros.',
    'Detê-lo ou descobrir quem lidera patrulhas inimigas que atacam comerciantes e suprimentos.',
    'Proteger e investigar um assentamento que caiu sob o olhar da Sombra.',
    'Enfrentar servos monstruosos do Inimigo e descobrir a arma que carregam.',
    'Descobrir o destino de um refúgio remoto dos Patrulheiros do qual não chegam notícias.',
  ],
  'tom-goldberry':[
    'Curar uma aflição sombria que atingiu criaturas do ermo, encontrando o ingrediente raro necessário.',
    'Purificar uma fonte sagrada contaminada pela Sombra e enfrentar seu guardião.',
    'Escoltar viajantes pelo ermo e descobrir a verdadeira importância de sua missão.',
    'Expurgar a presença que corrompeu um local sagrado.',
    'Encontrar e proteger uma flor preciosa que voltou a florescer depois de muitos anos.',
    'Libertar uma árvore antiga e sagrada de uma presença desagradável.',
  ],
};

export function patronMissionTable(value:string):{patron:PatronMissionPatron;missions:string[]}|null{
  const profile=patronProfile(value);
  if(profile.id==='balin'||profile.id==='bilbo'||profile.id==='cirdan'||profile.id==='gandalf'||profile.id==='gilraen'||profile.id==='tom-goldberry'){
    return {patron:profile.id,missions:PATRON_MISSIONS[profile.id]};
  }
  return null;
}

export function patronMissionAt(value:string,die?:number):{patron:PatronMissionPatron;die:number;prompt:string}{
  const table=patronMissionTable(value);
  if(!table)throw new Error('O Patrono atual não possui tabela de Missões do Modo Peregrino.');
  const d=die===undefined?secureRoll(1,6):Math.max(1,Math.min(6,Math.trunc(die)));
  return {patron:table.patron,die:d,prompt:table.missions[d-1]};
}

export type BrawlWeapon='unarmed'|'dagger'|'cudgel'|'club'|'improvised';
export const BRAWL_WEAPONS:Record<BrawlWeapon,{name:string;damage:number;injury:number|null;piercing:boolean}>={
  unarmed:{name:'Desarmado',damage:1,injury:null,piercing:false},
  dagger:{name:'Adaga',damage:2,injury:14,piercing:true},
  cudgel:{name:'Cassetete',damage:3,injury:12,piercing:true},
  club:{name:'Tacape',damage:4,injury:14,piercing:true},
  improvised:{name:'Arma improvisada / pedra',damage:1,injury:null,piercing:false},
};


export function assignedMountVigour(company:{mounts?:Array<{riderCharacterId?:string;vigour:number}>},characterId:string):number{
  const mount=(company.mounts||[]).find(item=>item.riderCharacterId===characterId);
  return mount?Math.max(0,Math.min(3,Math.trunc(Number(mount.vigour)||0))):0;
}

export function totalMountTreasureCapacity(company:{mounts?:Array<{treasureLoad:number}>}):number{
  return (company.mounts||[]).length*10;
}

export function usedMountTreasureCapacity(company:{mounts?:Array<{treasureLoad:number}>}):number{
  return (company.mounts||[]).reduce((sum,item)=>sum+Math.max(0,Math.min(10,Number(item.treasureLoad)||0)),0);
}
