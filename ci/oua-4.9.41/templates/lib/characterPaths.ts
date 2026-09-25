import type { Character } from './db';
export type CharacterPathCultureId = 'bardings'|'dwarves'|'elves'|'hobbits'|'bree'|'rangers';

export interface CharacterPathBackground {
  id: string;
  cultureId: CharacterPathCultureId;
  die: 1|2|3|4|5|6;
  title: string;
  attributes: [number,number,number];
  favouredSkill: string;
  distinctiveFeatures: [string,string];
}

export const CHARACTER_PATH_BACKGROUNDS: CharacterPathBackground[] = [
  {id:'bardings-1-hammer-anvil',cultureId:'bardings',die:1,title:'Pelo Martelo e Bigorna',attributes:[5,7,2],favouredSkill:'Atletismo',distinctiveFeatures:['Orgulhoso','Teimoso']},
  {id:'bardings-2-word-weaver',cultureId:'bardings',die:2,title:'Tecelão de Palavras',attributes:[4,6,4],favouredSkill:'Indução',distinctiveFeatures:['Ansioso','Belo']},
  {id:'bardings-3-blessed-senses',cultureId:'bardings',die:3,title:'Sentidos Abençoados',attributes:[5,5,4],favouredSkill:'Atletismo',distinctiveFeatures:['Bravo','Generoso']},
  {id:'bardings-4-healing-hands',cultureId:'bardings',die:4,title:'Mãos Curandeiras',attributes:[4,7,3],favouredSkill:'Indução',distinctiveFeatures:['Generoso','Alto']},
  {id:'bardings-5-dragon-stories',cultureId:'bardings',die:5,title:'Histórias de Dragões',attributes:[5,6,3],favouredSkill:'Indução',distinctiveFeatures:['Ansioso','Orgulhoso']},
  {id:'bardings-6-patient-hunter',cultureId:'bardings',die:6,title:'Um Caçador Paciente',attributes:[6,6,2],favouredSkill:'Atletismo',distinctiveFeatures:['Bravo','Feroz']},

  {id:'dwarves-1-life-labour',cultureId:'dwarves',die:1,title:'Uma Vida de Labor',attributes:[7,2,5],favouredSkill:'Ofício',distinctiveFeatures:['Sigiloso','Teimoso']},
  {id:'dwarves-2-distant-merchant',cultureId:'dwarves',die:2,title:'Mercador Distante',attributes:[6,2,6],favouredSkill:'Viagem',distinctiveFeatures:['Astuto','Orgulhoso']},
  {id:'dwarves-3-bitter-exile',cultureId:'dwarves',die:3,title:'Exílio Amargo',attributes:[7,3,4],favouredSkill:'Viagem',distinctiveFeatures:['Feroz','Orgulhoso']},
  {id:'dwarves-4-eloquent-speaker',cultureId:'dwarves',die:4,title:'Orador Eloquente',attributes:[5,4,5],favouredSkill:'Ofício',distinctiveFeatures:['Astuto','Nobre']},
  {id:'dwarves-5-grief-azanulbizar',cultureId:'dwarves',die:5,title:'O Pesar de Azanulbizar',attributes:[6,3,5],favouredSkill:'Viagem',distinctiveFeatures:['Feroz','Severo']},
  {id:'dwarves-6-piercing-gaze',cultureId:'dwarves',die:6,title:'Um Olhar Penetrante',attributes:[6,4,4],favouredSkill:'Ofício',distinctiveFeatures:['Cauteloso','Teimoso']},

  {id:'elves-1-mountain-visitor',cultureId:'elves',die:1,title:'Visitante das Montanhas',attributes:[5,2,7],favouredSkill:'História',distinctiveFeatures:['Belo','Ágil']},
  {id:'elves-2-sky-watcher',cultureId:'elves',die:2,title:'Vigia dos Céus',attributes:[4,3,7],favouredSkill:'História',distinctiveFeatures:['Olhar Aguçado','Cauteloso']},
  {id:'elves-3-ship-builder',cultureId:'elves',die:3,title:'Construtor de Navios',attributes:[5,3,6],favouredSkill:'Música',distinctiveFeatures:['Belo','Nobre']},
  {id:'elves-4-call-sea',cultureId:'elves',die:4,title:'O Chamado do Mar',attributes:[4,4,6],favouredSkill:'Música',distinctiveFeatures:['Paciente','Sutil']},
  {id:'elves-5-tower-watch',cultureId:'elves',die:5,title:'Vigia da Torre',attributes:[5,4,5],favouredSkill:'História',distinctiveFeatures:['Olhar Aguçado','Alegre']},
  {id:'elves-6-merchant-family',cultureId:'elves',die:6,title:'Uma Família Mercante',attributes:[6,2,6],favouredSkill:'Música',distinctiveFeatures:['Nobre','Cauteloso']},

  {id:'hobbits-1-restless-farmer',cultureId:'hobbits',die:1,title:'Fazendeiro Inquieto',attributes:[3,6,5],favouredSkill:'Furtividade',distinctiveFeatures:['Fiel','Rústico']},
  {id:'hobbits-2-many-roads',cultureId:'hobbits',die:2,title:'Muitos Caminhos a Trilhar',attributes:[4,5,5],favouredSkill:'Cortesia',distinctiveFeatures:['Ansioso','Alegre']},
  {id:'hobbits-3-bounder',cultureId:'hobbits',die:3,title:'Na Patrulha',attributes:[3,7,4],favouredSkill:'Cortesia',distinctiveFeatures:['Inquisitivo','Olhar Aguçado']},
  {id:'hobbits-4-witty-gentlehobbit',cultureId:'hobbits',die:4,title:'Espirituoso Hobbit Gentil',attributes:[2,6,6],favouredSkill:'Cortesia',distinctiveFeatures:['Eloquente','Alegre']},
  {id:'hobbits-5-bucklander',cultureId:'hobbits',die:5,title:'Buquense',attributes:[4,6,4],favouredSkill:'Furtividade',distinctiveFeatures:['Olhar Aguçado','Rústico']},
  {id:'hobbits-6-took-blood',cultureId:'hobbits',die:6,title:'Sangue Tûk',attributes:[2,7,5],favouredSkill:'Furtividade',distinctiveFeatures:['Ansioso','Honrado']},

  {id:'bree-1-crossroads-north',cultureId:'bree',die:1,title:'Encruzilhadas do Norte',attributes:[2,5,7],favouredSkill:'Discernimento',distinctiveFeatures:['Fiel','Generoso']},
  {id:'bree-2-dwarves',cultureId:'bree',die:2,title:'Partindo com os Anões',attributes:[3,4,7],favouredSkill:'Enigma',distinctiveFeatures:['Eloquente','Fiel']},
  {id:'bree-3-greenway-up',cultureId:'bree',die:3,title:'Estrada Verde Acima',attributes:[3,5,6],favouredSkill:'Discernimento',distinctiveFeatures:['Eloquente','Coração Verdadeiro']},
  {id:'bree-4-no-longer-carefree',cultureId:'bree',die:4,title:'Não Mais Livre de Cuidado e Medo',attributes:[4,4,6],favouredSkill:'Enigma',distinctiveFeatures:['Inquisitivo','Rústico']},
  {id:'bree-5-forest-dweller',cultureId:'bree',die:5,title:'Habitante da Floresta',attributes:[4,5,5],favouredSkill:'Enigma',distinctiveFeatures:['Astuto','Rústico']},
  {id:'bree-6-gate-guard',cultureId:'bree',die:6,title:'Guarda do Portão',attributes:[2,6,6],favouredSkill:'Discernimento',distinctiveFeatures:['Inquisitivo','Paciente']},

  {id:'rangers-1-orc-hunter',cultureId:'rangers',die:1,title:'Caçador de Orcs',attributes:[7,5,2],favouredSkill:'Caçada',distinctiveFeatures:['Bravo','Severo']},
  {id:'rangers-2-lore-keeper',cultureId:'rangers',die:2,title:'Guardião do Saber',attributes:[7,4,3],favouredSkill:'História',distinctiveFeatures:['Honrado','Coração Verdadeiro']},
  {id:'rangers-3-border-watch',cultureId:'rangers',die:3,title:'Vigilante da Fronteira',attributes:[6,5,3],favouredSkill:'Caçada',distinctiveFeatures:['Sigiloso','Alto']},
  {id:'rangers-4-far-herald',cultureId:'rangers',die:4,title:'Arauto de Longas Distâncias',attributes:[6,4,4],favouredSkill:'Caçada',distinctiveFeatures:['Ágil','Coração Verdadeiro']},
  {id:'rangers-5-counsellor',cultureId:'rangers',die:5,title:'Conselheiro',attributes:[5,5,4],favouredSkill:'História',distinctiveFeatures:['Sigiloso','Sutil']},
  {id:'rangers-6-land-warden',cultureId:'rangers',die:6,title:'Protetor da Terra',attributes:[6,6,2],favouredSkill:'História',distinctiveFeatures:['Bravo','Honrado']},
];

export type ImportantEventId =
  | 'shadow-haunted'|'destitute'|'half-wise'|'grim'|'inept'|'reclusive'
  | 'jovial'|'slender'|'simpleton'|'vigorous'|'blessed'|'grey-wizard';

export interface ImportantEvent {
  id: ImportantEventId;
  feat: 1|2|3|4|5|6|7|8|9|10|11|12;
  title: string;
  previousExperienceDelta?: number;
  fellowshipDelta?: number;
  lifestyleDelta?: number;
  hopeMaxDelta?: number;
  enduranceMaxDelta?: number;
  parryDelta?: number;
  fixedTnModifier?: { attribute:'strength'|'heart'|'wits'; amount:number };
  chooseTnImprovement?: boolean;
  addFavouredSkills?: number;
  removeFavouredSkills?: number;
  startsWithFlaw?: boolean;
  featOneBecomesEye?: boolean;
  eyeAwarenessDelta?: number;
}

export const IMPORTANT_EVENTS: ImportantEvent[] = [
  {id:'shadow-haunted',feat:11,title:'Perseguido pela Sombra',previousExperienceDelta:10,startsWithFlaw:true},
  {id:'destitute',feat:1,title:'Destituído',previousExperienceDelta:5,lifestyleDelta:-1},
  {id:'half-wise',feat:2,title:'Meio-sábio',fixedTnModifier:{attribute:'wits',amount:1},chooseTnImprovement:true},
  {id:'grim',feat:3,title:'Sombrio',fixedTnModifier:{attribute:'heart',amount:1},chooseTnImprovement:true},
  {id:'inept',feat:4,title:'Inepto',fixedTnModifier:{attribute:'strength',amount:1},chooseTnImprovement:true},
  {id:'reclusive',feat:5,title:'Recluso',fellowshipDelta:-1,addFavouredSkills:1},
  {id:'jovial',feat:6,title:'Jovial',fellowshipDelta:1,removeFavouredSkills:1},
  {id:'slender',feat:7,title:'Esbelto',enduranceMaxDelta:-2,parryDelta:1},
  {id:'simpleton',feat:8,title:'Simplório',hopeMaxDelta:2,removeFavouredSkills:2},
  {id:'vigorous',feat:9,title:'Vigoroso',enduranceMaxDelta:2,parryDelta:-1},
  {id:'blessed',feat:10,title:'Abençoado',previousExperienceDelta:-5,lifestyleDelta:1},
  {id:'grey-wizard',feat:12,title:'Favorecido pelo Mago Cinzento',featOneBecomesEye:true,eyeAwarenessDelta:2},
];

export function backgroundsForCulture(cultureId: CharacterPathCultureId): CharacterPathBackground[] {
  return CHARACTER_PATH_BACKGROUNDS.filter(background=>background.cultureId===cultureId);
}

export function backgroundById(id: string): CharacterPathBackground | undefined {
  return CHARACTER_PATH_BACKGROUNDS.find(background=>background.id===id);
}

export function importantEventByFeat(feat: number): ImportantEvent {
  const numeric=Math.max(1,Math.min(12,Math.trunc(Number(feat)||1));
  return IMPORTANT_EVENTS.find(event=>event.feat===numeric) || IMPORTANT_EVENTS[0];
}

export function shiftLifestyle(value: 'Frugal'|'Comum'|'Próspero', delta: number): 'Frugal'|'Comum'|'Próspero' {
  const order:['Frugal','Comum','Próspero']=['Frugal','Comum','Próspero'];
  const index=order.indexOf(value);
  return order[Math.max(0,Math.min(order.length-1,index+Math.trunc(Number(delta)||0)))];
}


export function importantEventForCharacter(character:Character): ImportantEvent | null {
  const feat=Number(character.sheet.other.characterPath?.importantEventFeat);
  if(!feat) return null;
  return importantEventByFeat(feat);
}

export function characterPathCompanyDeltas(characters:Character[],partyIds:string[]):{fellowship:number;eye:number} {
  const party=new Set(partyIds);
  let fellowship=0,eye=0;
  for(const character of characters){
    if(!party.has(character.id)) continue;
    const event=importantEventForCharacter(character);
    if(!event) continue;
    fellowship+=Number(event.fellowshipDelta)||0;
    eye+=Number(event.eyeAwarenessDelta)||0;
  }
  return {fellowship,eye};
}

export function featOneBecomesEye(character:Character|undefined|null):boolean {
  return Boolean(character && importantEventForCharacter(character)?.featOneBecomesEye);
}
