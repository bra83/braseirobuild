import { secureRoll } from '../dice';

export type HoardRating = 'lesser'|'greater'|'marvellous';
export type MagicalTreasureKind = 'magnificent-artefact'|'wondrous-item'|'famous-war-gear';
export type MagicalOrigin = 'Elven'|'Dwarven'|'Numenorean';
export type Bane = 'Orcs'|'Trolls'|'Wolves'|'Evil Men'|'Undead'|'Spiders';

export interface HoardRule {
  treasureDice: number;
  magicalFeatRolls: number;
}
export const HOARD_RULES: Record<HoardRating,HoardRule> = {
  lesser:{treasureDice:1,magicalFeatRolls:2},
  greater:{treasureDice:2,magicalFeatRolls:4},
  marvellous:{treasureDice:3,magicalFeatRolls:6},
};

export interface MagicalTreasureDiscovery {
  featResult: number;
  found: boolean;
  foundOnEye: boolean;
  kind?: MagicalTreasureKind;
  greedShadow?: number;
  natureDie?: number;
}

export function rollMagicalTreasureDiscoveries(rating:HoardRating): MagicalTreasureDiscovery[] {
  const rolls=HOARD_RULES[rating].magicalFeatRolls;
  return Array.from({length:rolls},()=>{
    const feat=secureRoll(1,12);
    const found=feat===11||feat===12;
    if(!found) return {featResult:feat,found:false,foundOnEye:false};
    const nature=secureRoll(1,6);
    const kind:MagicalTreasureKind=nature<=3?'magnificent-artefact':nature<=5?'wondrous-item':'famous-war-gear';
    const greedShadow=feat===11?(kind==='magnificent-artefact'?1:kind==='wondrous-item'?2:3):0;
    return {featResult:feat,found:true,foundOnEye:feat===11,kind,greedShadow,natureDie:nature};
  });
}

export function rollHoardTreasureValuePerHero(rating:HoardRating): number {
  return Array.from({length:HOARD_RULES[rating].treasureDice},()=>secureRoll(1,6)).reduce((a,b)=>a+b,0);
}

export const BLESSING_SKILLS = [
  ['Fascínio','Indução','Persuasão'],
  ['Atletismo','Viagem','Furtividade'],
  ['Vigilância','Discernimento','Busca'],
  ['Caçada','Cura','Exploração'],
  ['Cortesia','Música','Enigma'],
  ['Ofício','Batalha','História'],
] as const;

export function rollBlessingSkill(): string {
  const group=secureRoll(1,6)-1;
  const within=secureRoll(1,6);
  const slot=within<=2?0:within<=4?1:2;
  return BLESSING_SKILLS[group][slot];
}

export interface EnchantedQualityRule {
  id:string;
  name:string;
  origins:Array<MagicalOrigin|'Any'>;
  item:string;
  bane:'required'|'if-elven'|'if-numenorean'|'none';
  summary:string;
}
export const ENCHANTED_QUALITIES: EnchantedQualityRule[] = [
  {id:'hollow-steel',name:'Aço Oco',origins:['Numenorean'],item:'Arcos',bane:'none',summary:'Sempre permite uma saraivada de abertura adicional, mesmo quando nenhuma seria permitida, salvo se o portador estiver surpreso.'},
  {id:'superior-keen',name:'Afiada Superior',origins:['Dwarven','Elven'],item:'Qualquer arma',bane:'if-elven',summary:'Anã: Golpe Perfurante em 8+. Élfica: em 9+, ou 10−Valor contra criatura Flagelo.'},
  {id:'ancient-close-fitting',name:'Ajustado Ancestral',origins:['Elven','Dwarven'],item:'Armadura ou Elmo',bane:'none',summary:'Jogadas de Proteção somam 3 ou Valor, o que for maior.'},
  {id:'rune-scored-weapon',name:'Arma Entalhada com Runas',origins:['Dwarven','Elven'],item:'Qualquer arma',bane:'none',summary:'Ataques com a arma ignoram os efeitos de estar Arrasado ou Exausto.'},
  {id:'rune-scored-armour',name:'Armadura Entalhada com Runas',origins:['Dwarven'],item:'Armadura',bane:'none',summary:'Proteção com a armadura ignora os efeitos de estar Arrasado ou Exausto.'},
  {id:'mithril-armour',name:'Armadura de Mithril',origins:['Dwarven'],item:'Armadura de Malha',bane:'none',summary:'Camisa de malha tem Carga 3; cota de malha tem Carga 6.'},
  {id:'terror-shine',name:'Brilho de Terror',origins:['Dwarven'],item:'Arma corpo a corpo',bane:'none',summary:'Ao acertar, o alvo também perde 2 Ódio ou Determinação.'},
  {id:'flame-of-hope',name:'Chama da Esperança',origins:['Dwarven'],item:'Arma corpo a corpo',bane:'none',summary:'Ao acertar, todos na Companhia recuperam 1 Resistência +1 por ícone de Sucesso.'},
  {id:'foe-slayer',name:'Destruidora de Inimigos',origins:['Elven','Numenorean'],item:'Qualquer arma',bane:'required',summary:'Golpe Perfurante contra Flagelo torna Proteção Desfavorecida; se já Desfavorecida, causa Ferimento automático.'},
  {id:'rune-scored-shield',name:'Escudo Entalhado com Runas',origins:['Dwarven'],item:'Escudo',bane:'none',summary:'Ataques contra o portador são feitos como se o adversário estivesse Exausto.'},
  {id:'rune-scored-helm',name:'Elmo Entalhado com Runas',origins:['Dwarven'],item:'Elmo',bane:'none',summary:'Tarefas de Combate ignoram estar Arrasado ou Exausto.'},
  {id:'ancient-cunning-make',name:'Fabricação Habilidosa Ancestral',origins:['Elven','Dwarven'],item:'Armadura, Elmo ou Escudo',bane:'none',summary:'Reduz Carga em 3 ou Valor, o que for maior, mínimo 0.'},
  {id:'luminous',name:'Luminosa',origins:['Elven'],item:'Arma corpo a corpo',bane:'required',summary:'Brilha perto de criatura Flagelo; Companhia obtém sucesso automático para evitar emboscadas dessas criaturas.'},
  {id:'superior-grievous',name:'Cruel Superior',origins:['Elven','Numenorean'],item:'Qualquer arma',bane:'if-numenorean',summary:'Élfica: +4 Ferimento. Númenóreana: +2, ou +Valor contra Flagelo, o que for maior.'},
  {id:'very-fell',name:'Muito Atroz',origins:['Dwarven','Numenorean'],item:'Qualquer arma',bane:'if-numenorean',summary:'Anã: +2 Dano. Númenóreana: +1, ou +Valor contra Flagelo, o que for maior.'},
  {id:'cleaving',name:'Perfuradora',origins:['Any'],item:'Arma corpo a corpo',bane:'none',summary:'Ao matar um inimigo, pode atacar imediatamente um segundo adversário engajado.'},
  {id:'heavy',name:'Pesada',origins:['Any'],item:'Arma corpo a corpo',bane:'none',summary:'Alvo que perde Resistência ≥ 2× seu nível de Atributo é derrubado e gasta a próxima ação principal para levantar.'},
  {id:'bitter-missile',name:'Projétil Mordaz',origins:['Elven'],item:'Arma à distância',bane:'required',summary:'Ao acertar, alvo perde 1 Ódio/Determinação, ou 3 se for Flagelo.'},
  {id:'superior-reinforced',name:'Reforçado Superior',origins:['Any'],item:'Escudo',bane:'if-numenorean',summary:'+2 Bloqueio; se Élfico/Númenóreano, +1 ou +Valor contra Flagelo, o que for maior.'},
  {id:'straight-flight',name:'Voo Direto',origins:['Any'],item:'Arma à distância',bane:'none',summary:'Ignora modificadores negativos de ataque causados por vento, escuridão ou complicações semelhantes.'},
];

export type CurseId =
  | 'ill-luck'|'hunted'|'weakening'|'darkening'|'shadow-taint'
  | 'weakness-curse'|'malice'|'ill-omen'|'ownership';

export const CURSES: Array<{id:CurseId;name:string;summary:string}> = [
  {id:'ill-luck',name:'Azar',summary:'O resultado Olho no Dado de Façanha causa falha automática ao portador, como se estivesse Arrasado.'},
  {id:'hunted',name:'Caçado',summary:'Um inimigo escolhido percebe a presença do item quando se aproxima; Eventos de Jornada podem girar em torno da caçada.'},
  {id:'weakening',name:'Enfraquecimento',summary:'Aumenta em 2 o NA de Força, Coração ou Astúcia escolhido.'},
  {id:'darkening',name:'Escurecer',summary:'Quando revelado, jogadas apropriadas do portador perdem 1d enquanto a escuridão sobrenatural persistir.'},
  {id:'shadow-taint',name:'Mácula da Sombra',summary:'Sombra efetiva aumenta 1 por Bênção, 2 por duas Bênçãos; em equipamento famoso, +2 por Recompensa Encantada ativa. Não pode ser removida enquanto a Maldição persistir.'},
  {id:'weakness-curse',name:'Maldição da Fraqueza',summary:'Impõe temporariamente a pior Falha do Caminho da Sombra do portador; não conta para Sucumbir à Sombra.'},
  {id:'malice',name:'Malícia',summary:'Em jogadas relacionadas ao item, o portador não pode gastar Esperança para dados bônus.'},
  {id:'ill-omen',name:'Mau Agouro',summary:'Todas as jogadas do portador durante Conselhos perdem 1d.'},
  {id:'ownership',name:'Propriedade',summary:'Na presença do proprietário verdadeiro escolhido pelo Historiador, todas as características especiais do item ficam ineficazes.'},
];

export interface TreasureIndexEntry {
  id:string;
  destinedCharacterId:string;
  name:string;
  kind:MagicalTreasureKind;
  blessings:string[];
  origin?:MagicalOrigin;
  banes?:Bane[];
  qualities:string[];
  curse?:CurseId;
  curseTrigger?:string;
  curseRemoval?:string;
  notes?:string;
}

export function requiredBlessingCount(kind:MagicalTreasureKind):number {
  return kind==='magnificent-artefact'?1:kind==='wondrous-item'?2:0;
}

export function magicalLoad(kind:MagicalTreasureKind, normalGearLoad=0):number {
  return kind==='famous-war-gear'?Math.max(0,normalGearLoad):1;
}

export function blessingBonusDice(blessings:string[]|undefined, skill:string):number {
  const target=String(skill).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
  return (blessings||[]).some(value=>String(value).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase()===target)?2:0;
}

export function canBlessingGrantMagicalSuccess(blessings:string[]|undefined, skill:string):boolean {
  return blessingBonusDice(blessings,skill)>0;
}

export function allowedBanes(origin:MagicalOrigin):Bane[] {
  if(origin==='Elven') return ['Orcs','Wolves','Spiders'];
  if(origin==='Numenorean') return ['Orcs','Trolls','Wolves','Evil Men','Undead'];
  return [];
}

export function maxBanes(origin:MagicalOrigin):number {
  return origin==='Numenorean'?2:origin==='Elven'?1:0;
}

export function qualityById(id:string):EnchantedQualityRule|undefined {
  return ENCHANTED_QUALITIES.find(q=>q.id===id);
}

export function activeQualityIds(all:string[]|undefined, unlocked:number|undefined):string[] {
  const count=Math.max(0,Math.min((all||[]).length,Math.trunc(Number(unlocked)||0)));
  return (all||[]).slice(0,count);
}
