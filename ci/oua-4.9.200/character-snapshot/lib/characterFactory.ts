import type { Character, CharacterSheet } from './db';
import { CANONICAL_SKILLS } from './skillRegistry';
import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';
import { backgroundById, importantEventByFeat, shiftLifestyle, type CharacterPathCultureId } from './characterPaths';
import { firstFlawForShadowPath } from './rules/shadow';
import { normalizeEquipmentMechanics } from './rules/equipment';
import { CORE_COMMON_VIRTUES, canonicalCoreCommonVirtue, hasCoreCommonVirtue } from './coreVirtues';
export { CANONICAL_SKILLS } from './skillRegistry';

export const CREATION_RULESET_VERSION = 'tor2e-source-bound-character-creation-v2';


type SkillName = typeof CANONICAL_SKILLS[number];
type Proficiency = 'Machados' | 'Arcos' | 'Lanças' | 'Espadas';
type CultureRule = {
  id: string; culture: string; lifestyle: 'Frugal'|'Comum'|'Próspero'; blessing: string; sourceId?: string;
  attributes: Array<[number,number,number]>; derived: [number,number,number];
  skills: number[]; culturalFavoured: SkillName; culturalFavouredOptions?: SkillName[]; prof2: Proficiency[];
  features: string[]; names: string[]; ages:[number,number]; usefulItems:number;
};

const CULTURES: CultureRule[] = [
  { id:'bardings', culture:'Bardingo', lifestyle:'Próspero', blessing:'Coração Valente — jogadas de Valor são Favorecidas.', attributes:[[5,7,2],[4,7,3],[5,6,3],[4,6,4],[5,5,4],[6,6,2]], derived:[20,8,12], skills:[1,2,3,1,1,0,0,2,1,2,0,1,1,2,0,1,2,1], culturalFavoured:'Atletismo', prof2:['Arcos','Espadas'], features:['Audaz','Ansioso','Belo','Feroz','Generoso','Orgulhoso','Alto','Obstinado'], names:['Aegir','Arn','Brandulf','Domarr','Egil','Erland','Ingrith','Hilda','Astrid','Nari'], ages:[18,45], usefulItems:3 },
  { id:'dwarves', culture:'Anão do Povo de Durin', lifestyle:'Próspero', blessing:'Resoluto — a Carga de armaduras e elmos é reduzida à metade, arredondando para cima.', attributes:[[7,2,5],[7,3,4],[6,3,5],[6,4,4],[5,4,5],[6,2,6]], derived:[22,8,10], skills:[2,0,0,1,3,0,0,0,3,0,0,2,1,1,2,2,1,1], culturalFavoured:'Ofício', prof2:['Machados','Espadas'], features:['Astuto','Feroz','Senhorial','Orgulhoso','Reservado','Severo','Cauteloso','Obstinado'], names:['Borin','Burin','Frár','Grór','Náli','Nár','Óin','Regin','Dís','Rúna'], ages:[50,100], usefulItems:3 },
  { id:'elves', culture:'Elfo de Lindon', lifestyle:'Frugal', blessing:'Perícia Élfica — se não estiver Arrasado, pode gastar 1 Esperança para obter um Sucesso Mágico numa jogada de Perícia.', attributes:[[5,2,7],[4,3,7],[5,3,6],[4,4,6],[5,4,5],[6,2,6]], derived:[20,8,12], skills:[2,1,0,2,0,3,2,0,0,0,1,0,2,0,0,2,0,3], culturalFavoured:'História', prof2:['Arcos','Lanças'], features:['Belo','Olhos Aguçados','Senhorial','Alegre','Paciente','Sutil','Veloz','Cauteloso'], names:['Amras','Belegon','Calanhir','Galdor','Ithildir','Míriel','Nimloth','Níniel','Aranel','Elanor'], ages:[100,300], usefulItems:1 },
  { id:'hobbits', culture:'Hobbit do Condado', lifestyle:'Comum', blessing:'Bom Senso Hobbit — jogadas de Sabedoria são Favorecidas; resistência à ganância segue a bênção cultural.', attributes:[[3,6,5],[3,7,4],[2,7,5],[4,6,4],[4,5,5],[2,6,6]], derived:[18,10,12], skills:[0,0,2,0,0,3,2,2,0,0,1,0,2,2,3,1,0,0], culturalFavoured:'Furtividade', prof2:['Arcos','Espadas'], features:['Ansioso','Boa Fala','Fiel','Honrado','Inquisitivo','Olhos Aguçados','Alegre','Rústico'], names:['Milo','Tobias','Bungo','Dora','Rosa','Pérola','Dália','Poppy','Holly','Myrtle'], ages:[33,60], usefulItems:2 },
  { id:'bree', culture:'Homem de Bri', lifestyle:'Comum', blessing:'Sangue de Bri — cada Homem de Bri na Companhia aumenta a Sociedade em 1.', attributes:[[2,5,7],[3,4,7],[3,5,6],[4,4,6],[4,5,5],[2,6,6]], derived:[20,10,10], skills:[0,2,2,1,1,1,1,2,1,1,0,1,1,3,2,2,0,0], culturalFavoured:'Cortesia', prof2:['Machados','Lanças'], features:['Astuto','Boa Fala','Fiel','Generoso','Inquisitivo','Paciente','Rústico','Coração Verdadeiro'], names:['Alfred','Artie','Bill','Carl','Giles','Harry','Tilly','Daisy','Flora','Violet'], ages:[16,40], usefulItems:2 },
  { id:'rangers', culture:'Patrulheiro do Norte', lifestyle:'Frugal', blessing:'Reis dos Homens — aumente em 1 um Atributo escolhido durante a criação.', attributes:[[7,5,2],[7,4,3],[6,5,3],[6,4,4],[5,5,4],[6,6,2]], derived:[20,6,14], skills:[1,0,0,2,2,2,2,0,1,2,2,2,0,0,0,0,2,2], culturalFavoured:'Caçada', prof2:['Lanças','Espadas'], features:['Audaz','Honrado','Reservado','Severo','Sutil','Veloz','Alto','Coração Verdadeiro'], names:['Adrahil','Amlaith','Baranor','Cirion','Damrod','Eradan','Gilraen','Ivorwen','Morwen','Narieth'], ages:[18,60], usefulItems:1 },
];

for (const supplemental of SUPPLEMENT_CULTURES) {
  CULTURES.push({
    ...supplemental,
    culturalFavoured: supplemental.culturalFavouredOptions[0] as SkillName,
    culturalFavouredOptions: [...supplemental.culturalFavouredOptions] as SkillName[],
  });
}

const CALLINGS = [
  { name:'Capitão', favoured:['Batalha','Indução','Persuasão'] as SkillName[], feature:'Liderança', shadow:'Sedução do Poder' },
  { name:'Campeão', favoured:['Atletismo','Fascínio','Caçada'] as SkillName[], feature:'Conhecimento do Inimigo', shadow:'Maldição da Vingança' },
  { name:'Mensageiro', favoured:['Cortesia','Música','Viagem'] as SkillName[], feature:'Conhecimento dos Povos', shadow:'Loucura Errante' },
  { name:'Sábio', favoured:['Ofício','História','Enigma'] as SkillName[], feature:'Rimas de Conhecimento', shadow:'Sedução dos Segredos' },
  { name:'Caçador de Tesouros', favoured:['Exploração','Vigilância','Furtividade'] as SkillName[], feature:'Arrombamento', shadow:'Doença do Dragão' },
  { name:'Guardião', favoured:['Vigilância','Cura','Discernimento'] as SkillName[], feature:'Conhecimento da Sombra', shadow:'Caminho do Desespero' },
];
export const CHARACTER_PATH_CALLINGS = CALLINGS.map(({name,favoured})=>({name,favoured:[...favoured]}));

const WEAPONS: Record<Proficiency,{name:string;damage:number;injury:number;load:number}> = {
  Machados:{name:'Machado',damage:5,injury:18,load:2}, Arcos:{name:'Arco',damage:3,injury:14,load:2},
  Lanças:{name:'Lança',damage:4,injury:14,load:3}, Espadas:{name:'Espada',damage:4,injury:16,load:2},
};
const USEFUL: Array<[SkillName,string]> = [['Cura','Estojo de curandeiro'],['Ofício','Ferramentas de ofício'],['Viagem','Mapa e notas de caminho'],['Exploração','Corda e ganchos'],['Cortesia','Traje apropriado'],['Vigilância','Luneta simples'],['História','Caderno de tradições'],['Caçada','Utensílios de caça']];
const VIRTUES = CORE_COMMON_VIRTUES;
const REWARDS = ['Afiada','Ajustado','Fabricação Habilidosa','Cruel','Atroz','Reforçado'] as const;

function pick<T>(a: readonly T[]):T{return a[Math.floor(Math.random()*a.length)];}
function sample<T>(a: readonly T[], n:number):T[]{const p=[...a];const out:T[]=[];while(out.length<n&&p.length)out.push(p.splice(Math.floor(Math.random()*p.length),1)[0]);return out;}
function uniqueName(base:string, existing:string[]):string{let n=base,i=2;const set=new Set(existing.map(x=>x.trim().toLocaleLowerCase('pt-BR')));while(set.has(n.toLocaleLowerCase('pt-BR')))n=`${base} ${i++}`;return n;}
function id(prefix:string){return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2,8)}`;}

function spendPreviousExperience(ranks:Record<string,number>, prof:Record<string,number>, budget=10):string[]{
  const trace:string[]=[]; const seen=new Set<string>();
  const solve=(budget:number):boolean=>{
    if(budget===0)return true;
    const key=`${budget}|${CANONICAL_SKILLS.map(n=>ranks[n]||0).join('')}|${Object.values(prof).join('')}`; if(seen.has(key))return false; seen.add(key);
    const candidates:Array<{kind:'skill'|'prof';name:string;cost:number}> = [];
    for(const name of CANONICAL_SKILLS){const next=(ranks[name]||0)+1;if(next<=4){const cost=[0,1,2,3,5][next];if(cost<=budget)candidates.push({kind:'skill',name,cost});}}
    for(const name of Object.keys(prof)){const next=(prof[name]||0)+1;if(next<=3){const cost=[0,2,4,6][next];if(cost<=budget)candidates.push({kind:'prof',name,cost});}}
    // Embaralha escolhas, mas a busca retrocede até encontrar uma combinação que gaste exatamente 10 pontos.
    candidates.sort(()=>Math.random()-.5);
    for(const c of candidates){
      const before=c.kind==='skill'?ranks[c.name]:prof[c.name];
      if(c.kind==='skill')ranks[c.name]=before+1; else prof[c.name]=before+1;
      trace.push(`${c.kind==='skill'?'Perícia':'Proficiência'} ${c.name} → ${before+1} (${c.cost} PE)`);
      if(solve(budget-c.cost))return true;
      trace.pop(); if(c.kind==='skill')ranks[c.name]=before; else prof[c.name]=before;
    }
    return false;
  };
  if(!solve(budget))throw new Error('Criação inválida: não foi possível distribuir exatamente '+budget+' pontos de Experiência Prévia.');
  return trace;
}

export function validateRuleLegalCharacter(character:Character):string[]{
  const issues:string[]=[]; const c=CULTURES.find(x=>x.culture===character.sheet.identity.culture); const call=CALLINGS.find(x=>x.name===character.sheet.identity.vocation);
  if(!c)issues.push('Cultura fora das Culturas Heroicas 2e habilitadas pelo firewall de fontes.'); if(!call)issues.push('Vocação fora das seis Vocações do Livro Básico.'); if(!c||!call)return issues;
  const s=character.sheet, attrs:[number,number,number]=[s.attributes.strength,s.attributes.heart,s.attributes.wits];
  if(!s.other.heirOrigin&&s.identity.lifestyle!==c.lifestyle)issues.push('Padrão de Vida não corresponde à Cultura.');
  if((s.other.culturalBlessing||'')!==c.blessing)issues.push('Bênção Cultural não corresponde à Cultura.');
  if((s.other.callingFeature||'')!==call.feature)issues.push('Característica da Vocação incorreta.');
  if((s.other.shadowPath||'')!==call.shadow)issues.push('Caminho da Sombra incorreto.');
  const distinctive=s.other.distinctiveFeatures||[];
  const strider=s.other.rulesMode==='strider';
  const culturalDistinctive=distinctive.filter(x=>x!=='Peregrino');
  if(culturalDistinctive.length!==2||culturalDistinctive.some(x=>!c.features.includes(x)))issues.push('Características Distintivas não pertencem à Cultura.');
  if(strider&&!distinctive.includes('Peregrino'))issues.push('Modo Peregrino exige a Característica Notável extra Peregrino.');
  if(!strider&&distinctive.includes('Peregrino'))issues.push('Peregrino só deve ser aplicado pelo Modo Peregrino.');
  if(s.identity.age<c.ages[0]||s.identity.age>c.ages[1])issues.push('Idade fora da faixa usada pela Cultura.');
  const getsAttributeBonus=c.id==='rangers'||c.id==='high-elves-rivendell'; const legalBase=getsAttributeBonus?c.attributes.some(row=>[0,1,2].some(i=>row.every((v,j)=>attrs[j]===v+(i===j?1:0)))):c.attributes.some(row=>row.every((v,i)=>attrs[i]===v));
  if(!legalBase)issues.push('Atributos não correspondem a uma linha legal da Cultura.');
  const expectedEndurance=s.attributes.strength+c.derived[0]+(hasCoreCommonVirtue(s.other.virtues,'Vigor',s.identity.culture)?2:0); if(s.attributes.enduranceMax!==expectedEndurance||s.attributes.enduranceCurrent!==expectedEndurance)issues.push('Resistência inicial não deriva da Cultura/Virtude.');
  const expectedHope=s.attributes.heart+c.derived[1]+(hasCoreCommonVirtue(s.other.virtues,'Confiança',s.identity.culture)?2:0); if(s.attributes.hopeMax!==expectedHope||s.attributes.hopeCurrent!==expectedHope)issues.push('Esperança inicial não deriva da Cultura/Virtude.');
  const baseParry=s.attributes.wits+c.derived[2]; const expectedParry=baseParry+(hasCoreCommonVirtue(s.other.virtues,'Agilidade',s.identity.culture)?1:0)+(s.other.rewards.includes('Reforçado')?2:0); if(s.combat.parry!==expectedParry)issues.push('Bloqueio não deriva da Cultura/Virtude/escudo.');
  if(s.attributes.valor!==1||s.attributes.wisdom!==1)issues.push('Valor e Sabedoria iniciais devem ser 1.');
  if(s.attributes.shadow!==0||s.attributes.fatigue!==0||s.attributes.wounded)issues.push('Sombra, Fadiga e Ferimento devem começar zerados.');
  if(s.identity.patron||s.identity.company)issues.push('Patrono/Companhia não devem ser inventados pelo gerador antes da formação da Companhia.');
  if(s.resources.coins!==0||s.resources.treasure!==0||s.resources.experience!==0||s.resources.adventurePoints!==0||s.resources.skillPoints!==0)issues.push('Recursos de progressão/tesouro devem começar zerados.');
  const byName=new Map(s.skills.map(x=>[x.name,x])); if(s.skills.length!==CANONICAL_SKILLS.length||CANONICAL_SKILLS.some(name=>!byName.has(name)))issues.push('A ficha deve conter exatamente as 18 Perícias canônicas.');
  const fav=new Set(s.skills.filter(x=>x.favorite).map(x=>x.name)); const cultureFavs=(c.culturalFavouredOptions?.length?c.culturalFavouredOptions:[c.culturalFavoured]); if(!cultureFavs.some(skill=>fav.has(skill)))issues.push('Perícia Favorecida cultural ausente.');
  const callingFavCount=call.favoured.filter(x=>fav.has(x)).length; if(callingFavCount<2)issues.push('Duas Perícias Favorecidas da Vocação são obrigatórias.');
  const skillCostForTarget=(from:number,to:number)=>{let cost=0;for(let rank=from+1;rank<=to;rank++)cost+=[0,1,2,3,5][rank]||999;return cost;};
  let skillXp=0; CANONICAL_SKILLS.forEach((name,i)=>{const rank=Number(byName.get(name)?.rank??-1),base=c.skills[i];if(rank<base)issues.push(`Perícia ${name} caiu abaixo do valor cultural.`);if(rank<0||rank>4)issues.push(`Perícia ${name} fora do limite inicial 0–4.`);if(rank>=base&&rank<=4)skillXp+=skillCostForTarget(base,rank);});
  const finalProf=s.combat.proficiencies||{}; const groups=(['Machados','Arcos','Lanças','Espadas'] as Proficiency[]);
  if(groups.some(g=>Number(finalProf[g]||0)<0||Number(finalProf[g]||0)>3))issues.push('Proficiência de Combate fora do limite inicial 0–3.');
  const profCostForTarget=(from:number,to:number)=>{let cost=0;for(let rank=from+1;rank<=to;rank++)cost+=[0,2,4,6][rank]||999;return cost;};
  let legalXp=false;
  for(const primary of c.prof2){for(const secondary of groups.filter(g=>g!==primary)){const initial:Record<Proficiency,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0};initial[primary]=2;initial[secondary]=1;let cost=skillXp,possible=true;for(const g of groups){const target=Number(finalProf[g]||0);if(target<initial[g]){possible=false;break;}cost+=profCostForTarget(initial[g],target);}if(possible&&cost===10)legalXp=true;}}
  const expectedXp=Math.max(0,Math.min(20,Math.trunc(Number(s.other.heirOrigin?.previousExperienceBudget ?? s.other.creationPreviousExperienceBudget ?? (s.other.rulesMode==='strider'?15:10)))));
  if(!legalXp || expectedXp!==10){
    legalXp=false;
    for(const primary of c.prof2){
      for(const secondary of groups.filter(g=>g!==primary)){
        const initial:Record<Proficiency,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0};
        initial[primary]=2;initial[secondary]=1;
        let cost=skillXp,possible=true;
        for(const g of groups){
          const target=Number(finalProf[g]||0);
          if(target<initial[g]){possible=false;break;}
          cost+=profCostForTarget(initial[g],target);
        }
        if(possible&&cost===expectedXp)legalXp=true;
      }
    }
  }
  if(!legalXp)issues.push('A distribuição final não corresponde a exatamente '+expectedXp+' pontos legais de Experiência Prévia.');
  const weaponGroups=new Map(s.combat.weapons.map(w=>[w.group,w])); for(const g of groups){if(Number(finalProf[g]||0)>0&&!weaponGroups.has(g))issues.push(`Falta uma arma inicial para a Proficiência ${g}.`);}
  if(s.combat.weapons.some(w=>!groups.includes(w.group as Proficiency)))issues.push('Arma inicial usa Proficiência não canônica.');
  if(c.id==='hobbits'&&s.combat.weapons.some(w=>!['Arco','Espada curta','Machado','Lança'].includes(w.name)))issues.push('Hobbit recebeu arma grande não permitida.');
  if(!CORE_COMMON_VIRTUES.some(v=>canonicalCoreCommonVirtue(s.other.virtues,s.identity.culture)===v))issues.push('Virtude inicial fora do catálogo source-bound do Livro Básico.');
  if(!['Afiada','Ajustado','Fabricação Habilidosa','Cruel','Atroz','Reforçado'].some(r=>s.other.rewards.includes(r)))issues.push('Recompensa inicial fora do subconjunto legal implementado.');
  if(s.inventory.filter(i=>i.category==='Item útil').length!==c.usefulItems)issues.push('Quantidade de Itens Úteis não corresponde ao Padrão de Vida.');
  if(!s.inventory.some(i=>i.category==='Armadura'))issues.push('Armadura inicial ausente.');
  if(s.other.rewards.includes('Reforçado')&&!s.inventory.some(i=>i.category==='Escudo'&&i.tags?.some(tag=>tag==='Recompensa:Reforçado')))issues.push('Recompensa Reforçado não está ligada a um escudo.');
  if(!s.inventory.some(i=>i.category==='Viagem'))issues.push('Equipamento de viagem inicial ausente.');
  const calculatedLoad=s.inventory.reduce((sum,item)=>sum+Number(item.load||0)*Math.max(1,Number(item.quantity||1)),0); if(s.attributes.load!==calculatedLoad)issues.push('Carga inicial não corresponde ao equipamento.');
  return Array.from(new Set(issues));
}

export function createRandomCharacter(existingNames:string[]=[], options:{rulesMode?:'standard'|'strider';previousExperienceBudget?:number}={}):Character{
  const culture=pick(CULTURES), calling=pick(CALLINGS), trace:string[]=[`Regras: ${CREATION_RULESET_VERSION}`,`Cultura: ${culture.culture}`,`Vocação: ${calling.name}`];
  const rulesMode=options.rulesMode==='strider'?'strider':'standard';
  const base=pick(culture.attributes); let [strength,heart,wits]=base;
  if(culture.id==='rangers'||culture.id==='high-elves-rivendell'){const target=Math.floor(Math.random()*3); if(target===0)strength++; else if(target===1)heart++; else wits++; trace.push(`${culture.id==='rangers'?'Reis dos Homens':'Elfo de Valfenda'}: +1 em ${['Força','Coração','Astúcia'][target]}.`);}
  const ranks:Record<string,number>={}; CANONICAL_SKILLS.forEach((n,i)=>ranks[n]=culture.skills[i]);
  const primary=pick(culture.prof2), other=pick((['Machados','Arcos','Lanças','Espadas'] as Proficiency[]).filter(x=>x!==primary));
  const prof:Record<string,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0,[primary]:2,[other]:1};
  const previousExperienceBudget=options.previousExperienceBudget!==undefined?Math.max(0,Math.min(20,Math.trunc(options.previousExperienceBudget))):(rulesMode==='strider'?15:10);
  trace.push(...spendPreviousExperience(ranks,prof,previousExperienceBudget));
  trace.push('Experiência Prévia: '+previousExperienceBudget+'/'+previousExperienceBudget+' pontos.');
  const culturalFavoured=pick(culture.culturalFavouredOptions?.length?culture.culturalFavouredOptions:[culture.culturalFavoured]); const callingPool=calling.favoured.filter(skill=>skill!==culturalFavoured); const callingFav=sample(callingPool.length>=2?callingPool:calling.favoured,2), favourite=new Set<string>([culturalFavoured,...callingFav]);
  const virtue=pick(VIRTUES); if(virtue==='Maestria'){for(const skill of sample(CANONICAL_SKILLS.filter(x=>!favourite.has(x)),2))favourite.add(skill);}
  let endurance=strength+culture.derived[0], hope=heart+culture.derived[1], parry=wits+culture.derived[2];
  let tnModifiers: Partial<Record<'strength'|'heart'|'wits',number>>|undefined;
  if(virtue==='Confiança')hope+=2; if(virtue==='Vigor')endurance+=2; if(virtue==='Agilidade')parry+=1;
  if(virtue==='Proficiência'){const key=pick(['strength','heart','wits'] as const); tnModifiers={[key]:-1}; trace.push(`Proficiência: NA de ${key} reduzido em 1.`);}
  const reward=pick(REWARDS), features=sample(culture.features,2), name=uniqueName(pick(culture.names),existingNames);
  if(rulesMode==='strider') features.push('Peregrino');
  const profEntries=(Object.entries(prof) as Array<[Proficiency,number]>).filter(([,v])=>v>0);
  const rewardNeedsWeapon=['Afiada','Cruel','Atroz'].includes(reward);
  const weapons:CharacterSheet['combat']['weapons']=profEntries.map(([p],i)=>{let baseWeapon={...WEAPONS[p]}; if(culture.id==='hobbits'&&p==='Espadas')baseWeapon={name:'Espada curta',damage:3,injury:16,load:1}; const w={name:baseWeapon.name,damage:baseWeapon.damage,injury:baseWeapon.injury,group:p,active:i===0}; if(i===0&&reward==='Atroz')w.damage+=1; if(i===0&&reward==='Cruel')w.injury+=2; return w;});
  const inventory:CharacterSheet['inventory']=weapons.map((w,i)=>({id:id('item'),name:w.name,quantity:1,weight:(culture.id==='hobbits'&&w.name==='Espada curta'?1:WEAPONS[w.group as Proficiency]?.load||1),load:(culture.id==='hobbits'&&w.name==='Espada curta'?1:WEAPONS[w.group as Proficiency]?.load||1),category:'Arma',description:i===0&&rewardNeedsWeapon?`Arma com Recompensa inicial: ${reward}.`:'Arma inicial correspondente à Proficiência de Combate.',equipped:i===0,damage:w.damage,injury:w.injury,tags:[w.group,...(i===0&&rewardNeedsWeapon?[`Recompensa:${reward}`]:[])]}));
  weapons.forEach((weapon,index)=>{weapon.inventoryItemId=inventory[index]?.id;});
  const armor={id:id('item'),name:'Camisa de Couro',quantity:1,weight:3,load:3,category:'Armadura',description:'Armadura inicial.',equipped:true,protection:1,tags:['couro']};
  if(reward==='Ajustado') armor.tags.push('Recompensa:Ajustado','proteção:+2');
  if(reward==='Fabricação Habilidosa'){armor.tags.push('Recompensa:Fabricação Habilidosa');armor.load=1;armor.weight=1;}
  inventory.push(armor);
  if(reward==='Reforçado') inventory.push({id:id('item'),name:'Broquel',quantity:1,weight:2,load:2,category:'Escudo',description:'Escudo inicial com Recompensa Reforçado.',equipped:true,tags:['escudo','bloqueio:+2','Recompensa:Reforçado']});
  for(const [skill,label] of sample(USEFUL,culture.usefulItems))inventory.push({id:id('item'),name:label,quantity:1,weight:0,load:0,category:'Item útil',description:`Item útil associado a ${skill}; sua aplicação segue as regras de Itens Úteis.`,equipped:false,tags:[`Perícia:${skill}`]});
  inventory.push({id:id('item'),name:'Equipamento de viagem',quantity:1,weight:0,load:0,category:'Viagem',description:'Equipamento de viagem sazonal abstrato de O Um Anel; sem Carga individual.',equipped:false,tags:['Viagem']});
  const sheet:CharacterSheet={identity:{name,culture:culture.culture,vocation:calling.name,lifestyle:culture.lifestyle,age:Math.floor(culture.ages[0]+Math.random()*(culture.ages[1]-culture.ages[0]+1)),description:`${features.join(' e ')}.`,patron:'',company:''},attributes:{strength,heart,wits,enduranceCurrent:endurance,enduranceMax:endurance,hopeCurrent:hope,hopeMax:hope,shadow:0,fatigue:0,load:0,wounded:false,weary:false,miserable:false,supplies:100,valor:1,wisdom:1,protection:0,tnModifiers},skills:CANONICAL_SKILLS.map(name=>({name,rank:ranks[name],specialization:'',favorite:favourite.has(name)})),combat:{parry:parry+(reward==='Reforçado'?2:0),armor:'Camisa de Couro',helmet:'Nenhum',shield:reward==='Reforçado'?'Broquel':'Nenhum',proficiencies:prof,weapons},resources:{coins:0,treasure:0,experience:0,adventurePoints:0,skillPoints:0},inventory,other:{virtues:virtue,rewards:`${reward} (${reward==='Reforçado'?'Broquel':reward==='Ajustado'||reward==='Fabricação Habilidosa'?'Camisa de Couro':weapons[0]?.name||'arma inicial'})`,flaws:'',notes:`Herói criado automaticamente por fontes 2e autorizadas (${sourceForCulture(culture.culture)}). Experiência Prévia: ${previousExperienceBudget}/${previousExperienceBudget} pontos. Patrono e Refúgio Seguro ficam para a formação da Companhia.`,narrativeConditions:'Sem Ferimento; Sombra 0; Fadiga 0.',culturalBlessing:culture.blessing,shadowPath:calling.shadow,callingFeature:calling.feature,distinctiveFeatures:features,creationTrace:trace,rulesMode,creationPreviousExperienceBudget:previousExperienceBudget},tokenId:null,historyLog:[]};
  const normalizedSheet = normalizeEquipmentMechanics(sheet);
  const character:Character={id:id('char'),name,culture:culture.culture,vocation:calling.name,sheet:normalizedSheet};
  const issues=validateRuleLegalCharacter(character); if(issues.length)throw new Error(`Gerador recusou ficha fora das regras: ${issues.join(' ')}`);
  return character;
}

export const INITIAL_REWARDS = [...REWARDS] as const;
export const INITIAL_VIRTUES = [...VIRTUES] as const;
export const CORE_CREATION_SUMMARY = CULTURES.map(c=>({culture:c.culture,sourceId:c.sourceId||'tor2e-core',attributeRows:c.attributes.length,skillCount:c.skills.length,standard:c.lifestyle,culturalFavouredOptions:c.culturalFavouredOptions||[c.culturalFavoured],proficiencyChoices:[...c.prof2]}));


export interface CharacterPathCreationOptions {
  cultureId: CharacterPathCultureId;
  backgroundId: string;
  importantEventFeat: number;
  culturalAttributeBonus?: 'strength'|'heart'|'wits';
  existingNames?: string[];
  callingName?: string;
  callingFavouredSkills?: string[];
  rulesMode?: 'standard'|'strider';
  tnImprovementAttribute?: 'strength'|'heart'|'wits';
  addedFavouredSkills?: string[];
  removedFavouredSkills?: string[];
  coreVirtue?: (typeof CORE_COMMON_VIRTUES)[number];
  coreVirtueAttribute?: 'strength'|'heart'|'wits';
  coreVirtueMasterySkills?: string[];
}

export function createCharacterFromPath(options:CharacterPathCreationOptions):Character {
  const culture=CULTURES.find(item=>item.id===options.cultureId);
  if(!culture) throw new Error('Trilhas de Personagens cobre apenas as seis Culturas Heroicas do Livro Básico.');
  const background=backgroundById(options.backgroundId);
  if(!background||background.cultureId!==options.cultureId) throw new Error('Antecedente incompatível com a Cultura selecionada.');
  const event=importantEventByFeat(options.importantEventFeat);
  const calling=options.callingName ? CALLINGS.find(item=>item.name===options.callingName) : pick(CALLINGS);
  if(!calling) throw new Error('Chamado inválido.');
  const rulesMode=options.rulesMode==='strider'?'strider':'standard';
  const trace:string[]=[
    'Regras: '+CREATION_RULESET_VERSION+' + Trilhas de Personagens',
    'Cultura: '+culture.culture,
    'Antecedente: '+background.title,
    'Evento Importante: '+event.title+' (Façanha '+event.feat+')',
    'Vocação: '+calling.name,
  ];

  let [strength,heart,wits]=background.attributes;
  if(culture.id==='rangers'){
    const attribute=options.culturalAttributeBonus||(['strength','heart','wits'] as const)[Math.floor(Math.random()*3)];
    if(attribute==='strength')strength++; else if(attribute==='heart')heart++; else wits++;
    trace.push('Reis dos Homens: +1 em '+({strength:'Força',heart:'Coração',wits:'Astúcia'} as const)[attribute]+'.');
  } else if(options.culturalAttributeBonus) throw new Error('O bônus de atributo Reis dos Homens só se aplica aos Fronteiros do Norte.');

  const ranks:Record<string,number>={};
  CANONICAL_SKILLS.forEach((name,i)=>ranks[name]=culture.skills[i]);
  const primary=pick(culture.prof2);
  const groups=(['Machados','Arcos','Lanças','Espadas'] as Proficiency[]);
  const other=pick(groups.filter(group=>group!==primary));
  const prof:Record<string,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0,[primary]:2,[other]:1};

  const baseBudget=rulesMode==='strider'?15:10;
  const previousExperienceBudget=Math.max(0,baseBudget+Number(event.previousExperienceDelta||0));
  trace.push(...spendPreviousExperience(ranks,prof,previousExperienceBudget));
  trace.push('Experiência Prévia: '+previousExperienceBudget+'/'+previousExperienceBudget+' pontos.');

  // Antecedente replaces the cultural Favoured-skill choice.
  const callingPool=calling.favoured.filter(skill=>skill!==background.favouredSkill);
  const chosenCallingFavs=Array.from(new Set((options.callingFavouredSkills||[]).map(String)));
  if(chosenCallingFavs.length){
    if(chosenCallingFavs.length!==2||chosenCallingFavs.some(skill=>!callingPool.includes(skill as SkillName))) throw new Error('Escolha duas Perícias distintas do Chamado que ainda não estejam Favorecidas pela Cultura.');
  }
  const callingFav=(chosenCallingFavs.length?chosenCallingFavs:sample(callingPool.length>=2?callingPool:calling.favoured,2)) as SkillName[];
  const favourite=new Set<string>([background.favouredSkill,...callingFav]);

  const virtue=options.coreVirtue||pick(VIRTUES);
  if(!CORE_COMMON_VIRTUES.includes(virtue as (typeof CORE_COMMON_VIRTUES)[number])) throw new Error('Escolha uma das seis Virtudes Gerais do Livro Básico.');
  if(virtue==='Maestria'){
    const selected=Array.from(new Set((options.coreVirtueMasterySkills||[]).map(String)));
    if(selected.length!==2||selected.some(skill=>!CANONICAL_SKILLS.includes(skill as SkillName)||favourite.has(skill))) throw new Error('Maestria exige escolher duas Perícias diferentes que ainda não sejam Favorecidas.');
    for(const skill of selected) favourite.add(skill);
    trace.push('Maestria: Perícias Favorecidas escolhidas: '+selected.join(', ')+'.');
  } else if(options.coreVirtueMasterySkills?.length) throw new Error('Perícias para Maestria só podem ser escolhidas quando a Virtude for Maestria.');
  if(virtue==='Proficiência'&&!['strength','heart','wits'].includes(String(options.coreVirtueAttribute))) throw new Error('Proficiência exige escolher Força, Coração ou Astúcia para reduzir o NA em 1.');
  if(virtue!=='Proficiência'&&options.coreVirtueAttribute) throw new Error('A escolha de Atributo só se aplica à Virtude Proficiência.');

  let requestedAdds=Array.from(new Set((options.addedFavouredSkills||[]).map(String)));
  let requestedRemovals=Array.from(new Set((options.removedFavouredSkills||[]).map(String)));
  if(event.addFavouredSkills){
    if(requestedAdds.length===0){
      requestedAdds=sample(CANONICAL_SKILLS.filter(skill=>!favourite.has(skill)),event.addFavouredSkills);
      trace.push('Escolha automática legal de Perícia Favorecida adicional: '+requestedAdds.join(', ')+'.');
    }
    if(requestedAdds.length!==event.addFavouredSkills) throw new Error('O Evento '+event.title+' exige escolher '+event.addFavouredSkills+' Perícia adicional para ficar Favorecida.');
    for(const skill of requestedAdds){
      if(!CANONICAL_SKILLS.includes(skill as SkillName)) throw new Error('Perícia adicional inválida: '+skill);
      favourite.add(skill);
    }
  } else if(requestedAdds.length) throw new Error('Este Evento não permite adicionar Perícias Favorecidas.');

  if(event.removeFavouredSkills){
    if(requestedRemovals.length===0){
      requestedRemovals=sample(Array.from(favourite),event.removeFavouredSkills);
      trace.push('Escolha automática legal de Perícia(s) que deixa(m) de ser Favorecida(s): '+requestedRemovals.join(', ')+'.');
    }
    if(requestedRemovals.length!==event.removeFavouredSkills) throw new Error('O Evento '+event.title+' exige retirar '+event.removeFavouredSkills+' Perícia(s) Favorecida(s).');
    for(const skill of requestedRemovals){
      if(!favourite.has(skill)) throw new Error(skill+' não está Favorecida e não pode ser retirada.');
      favourite.delete(skill);
    }
  } else if(requestedRemovals.length) throw new Error('Este Evento não exige remover Perícias Favorecidas.');

  let endurance=strength+culture.derived[0];
  let hope=heart+culture.derived[1];
  let parry=wits+culture.derived[2];
  let tnModifiers:Partial<Record<'strength'|'heart'|'wits',number>>={};
  if(virtue==='Confiança') hope+=2;
  if(virtue==='Vigor') endurance+=2;
  if(virtue==='Agilidade') parry+=1;
  if(virtue==='Proficiência'){
    const key=options.coreVirtueAttribute!;
    tnModifiers[key]=(tnModifiers[key]||0)-1;
    trace.push('Proficiência: NA de '+key+' reduzido em 1.');
  }

  if(event.fixedTnModifier){
    tnModifiers[event.fixedTnModifier.attribute]=(tnModifiers[event.fixedTnModifier.attribute]||0)+event.fixedTnModifier.amount;
  }
  if(event.chooseTnImprovement){
    const penalized=event.fixedTnModifier?.attribute;
    const chosen=options.tnImprovementAttribute || pick((['strength','heart','wits'] as const).filter(value=>value!==penalized));
    if(chosen===penalized) throw new Error(event.title+' exige escolher outro NA de Atributo para diminuir em 1.');
    tnModifiers[chosen]=(tnModifiers[chosen]||0)-1;
    if(!options.tnImprovementAttribute) trace.push('Escolha automática legal: NA de '+chosen+' reduzido em 1.');
  } else if(options.tnImprovementAttribute) throw new Error('Este Evento não pede uma escolha de redução de NA.');

  endurance=Math.max(1,endurance+Number(event.enduranceMaxDelta||0));
  hope=Math.max(1,hope+Number(event.hopeMaxDelta||0));
  parry=Math.max(0,parry+Number(event.parryDelta||0));

  const reward=pick(REWARDS);
  const features=[...background.distinctiveFeatures];
  if(rulesMode==='strider') features.push('Peregrino');
  const name=uniqueName(pick(culture.names),options.existingNames||[]);
  const lifestyle=shiftLifestyle(culture.lifestyle,Number(event.lifestyleDelta||0));
  const profEntries=(Object.entries(prof) as Array<[Proficiency,number]>).filter(([,value])=>value>0);
  const rewardNeedsWeapon=['Afiada','Cruel','Atroz'].includes(reward);

  const weapons:CharacterSheet['combat']['weapons']=profEntries.map(([group],index)=>{
    let baseWeapon={...WEAPONS[group]};
    if(culture.id==='hobbits'&&group==='Espadas') baseWeapon={name:'Espada curta',damage:3,injury:16,load:1};
    const weapon={name:baseWeapon.name,damage:baseWeapon.damage,injury:baseWeapon.injury,group,active:index===0};
    if(index===0&&reward==='Atroz') weapon.damage+=1;
    if(index===0&&reward==='Cruel') weapon.injury+=2;
    return weapon;
  });

  const inventory:CharacterSheet['inventory']=weapons.map((weapon,index)=>({
    id:id('item'),name:weapon.name,quantity:1,
    weight:(culture.id==='hobbits'&&weapon.name==='Espada curta'?1:WEAPONS[weapon.group as Proficiency]?.load||1),
    load:(culture.id==='hobbits'&&weapon.name==='Espada curta'?1:WEAPONS[weapon.group as Proficiency]?.load||1),
    category:'Arma',
    description:index===0&&rewardNeedsWeapon?'Arma com Recompensa inicial: '+reward+'.':'Arma inicial correspondente à Proficiência de Combate.',
    equipped:index===0,damage:weapon.damage,injury:weapon.injury,
    tags:[weapon.group,...(index===0&&rewardNeedsWeapon?['Recompensa:'+reward]:[])],
  }));
  weapons.forEach((weapon,index)=>{weapon.inventoryItemId=inventory[index]?.id;});
  const armor={id:id('item'),name:'Camisa de Couro',quantity:1,weight:3,load:3,category:'Armadura',description:'Armadura inicial.',equipped:true,protection:1,tags:['couro']};
  if(reward==='Ajustado') armor.tags.push('Recompensa:Ajustado','proteção:+2');
  if(reward==='Fabricação Habilidosa'){armor.tags.push('Recompensa:Fabricação Habilidosa');armor.load=1;armor.weight=1;}
  inventory.push(armor);
  if(reward==='Reforçado') inventory.push({id:id('item'),name:'Broquel',quantity:1,weight:2,load:2,category:'Escudo',description:'Escudo inicial com Recompensa Reforçado.',equipped:true,tags:['escudo','bloqueio:+2','Recompensa:Reforçado']});
  for(const [skill,label] of sample(USEFUL,culture.usefulItems)) inventory.push({id:id('item'),name:label,quantity:1,weight:0,load:0,category:'Item útil',description:'Item útil associado a '+skill+'; sua aplicação segue as regras de Itens Úteis.',equipped:false,tags:['Perícia:'+skill]});
  inventory.push({id:id('item'),name:'Equipamento de viagem',quantity:1,weight:0,load:0,category:'Viagem',description:'Equipamento de viagem sazonal abstrato de O Um Anel; sem Carga individual.',equipped:false,tags:['Viagem']});

  let flaws='';
  if(event.startsWithFlaw){
    const first=firstFlawForShadowPath(calling.shadow);
    if(!first) throw new Error('O Caminho da Sombra do Chamado não possui sequência canônica de Falhas.');
    flaws=first;
    trace.push('Perseguido pela Sombra: começa com a Falha '+first+'.');
  }

  const sheet:CharacterSheet={
    identity:{
      name,culture:culture.culture,vocation:calling.name,lifestyle,
      age:Math.floor(culture.ages[0]+Math.random()*(culture.ages[1]-culture.ages[0]+1)),
      description:features.join(' e ')+'.',patron:'',company:'',
    },
    attributes:{
      strength,heart,wits,enduranceCurrent:endurance,enduranceMax:endurance,
      hopeCurrent:hope,hopeMax:hope,shadow:0,fatigue:0,load:0,wounded:false,weary:false,miserable:false,
      supplies:100,valor:1,wisdom:1,protection:0,tnModifiers:Object.keys(tnModifiers).length?tnModifiers:undefined,
    },
    skills:CANONICAL_SKILLS.map(skill=>({name:skill,rank:ranks[skill],specialization:'',favorite:favourite.has(skill)})),
    combat:{parry:parry+(reward==='Reforçado'?2:0),armor:'Camisa de Couro',helmet:'Nenhum',shield:reward==='Reforçado'?'Broquel':'Nenhum',proficiencies:prof,weapons},
    resources:{coins:0,treasure:0,experience:0,adventurePoints:0,skillPoints:0},
    inventory,
    other:{
      virtues:virtue,
      rewards:reward+' ('+(reward==='Reforçado'?'Broquel':reward==='Ajustado'||reward==='Fabricação Habilidosa'?'Camisa de Couro':weapons[0]?.name||'arma inicial')+')',
      flaws,
      notes:'Herói criado por Trilhas de Personagens. Experiência Prévia: '+previousExperienceBudget+'/'+previousExperienceBudget+' pontos.',
      narrativeConditions:'Sem Ferimento; Sombra 0; Fadiga 0.',
      culturalBlessing:culture.blessing,shadowPath:calling.shadow,callingFeature:calling.feature,
      distinctiveFeatures:features,creationTrace:trace,rulesMode,
      characterPath:{
        backgroundId:background.id,importantEventId:event.id,importantEventFeat:event.feat,
        tnImprovementAttribute:options.tnImprovementAttribute,
        addedFavouredSkills:requestedAdds,removedFavouredSkills:requestedRemovals,
      },
    },
    tokenId:null,historyLog:[],
  };
  sheet.attributes.load=sheet.inventory.reduce((sum,item)=>sum+Number(item.load||0)*Math.max(1,Number(item.quantity)||1),0);
  const character:Character={id:id('char'),name,culture:culture.culture,vocation:calling.name,sheet};
  return character;
}

export const CHARACTER_PATH_CULTURES = CULTURES
  .filter(culture=>['bardings','dwarves','elves','hobbits','bree','rangers'].includes(culture.id))
  .map(culture=>({id:culture.id as CharacterPathCultureId,name:culture.culture}));

export function characterPathCultureForDie(die:number):CharacterPathCultureId{
  const culture=CHARACTER_PATH_CULTURES[Math.trunc(Number(die))-1];
  if(!Number.isInteger(die)||die<1||die>6||!culture)throw new Error('O dado de Cultura Heroica precisa resultar de 1 a 6.');
  return culture.id;
}


export interface HeirCreationOptions {
  predecessor: Character;
  heirName: string;
  previousExperienceBudget: number;
  inheritedFavouredSkill: string;
  inheritedItems?: CharacterSheet['inventory'];
  existingNames?: string[];
  rulesMode?: 'standard'|'strider';
}

export function createHeirCharacter(options:HeirCreationOptions):Character {
  const predecessor=options.predecessor;
  const budget=Math.max(10,Math.min(20,Math.trunc(Number(options.previousExperienceBudget)||0)));
  if(budget<10) throw new Error('O herdeiro precisa de pelo menos 10 pontos de Experiência Anterior para estar pronto.');
  const familySkill=String(options.inheritedFavouredSkill||'').trim();
  const sourceSkill=predecessor.sheet.skills.find(skill=>skill.name===familySkill&&skill.favorite);
  if(!sourceSkill) throw new Error('A Perícia de herança familiar deve ser Favorecida no Herói original.');

  const heir=createRandomCharacter(options.existingNames||[],{
    rulesMode:options.rulesMode,
    previousExperienceBudget:budget,
  });
  const sheet=structuredClone(heir.sheet);
  sheet.identity.name=String(options.heirName||'').trim()||heir.sheet.identity.name;
  sheet.identity.lifestyle=predecessor.sheet.identity.lifestyle;
  sheet.skills=sheet.skills.map(skill=>skill.name===familySkill?{...skill,favorite:true}:skill);

  const inherited=(options.inheritedItems||[]).map(item=>structuredClone(item));
  for(const item of inherited){
    item.id=id('inheritance');
    item.tags=Array.from(new Set([...(item.tags||[]),'Herança']));
    item.equipped=false;
    // Heranças mantêm todas as Recompensas/qualidades já despertadas no item.
    // Artefatos abençoados revelam suas Bênçãos ao herdeiro.

    if(item.magicalKind==='magnificent-artefact'||item.magicalKind==='wondrous-item'){
      item.magicalIdentified=true;
    }
  }
  sheet.inventory=[...sheet.inventory,...inherited];
  for(const item of inherited){
    if(String(item.category||'').toLowerCase()==='arma'){
      const group=(item.tags||[]).find(tag=>['Machados','Arcos','Lanças','Espadas'].includes(tag)) as Proficiency|undefined;
      if(group){
        sheet.combat.weapons=[...(sheet.combat.weapons||[]),{
          name:item.name,
          damage:Number(item.damage)||0,
          injury:Number(item.injury)||0,
          group,
          active:false,
          magicalBaseItemName:item.magicalBaseItemName,
          treasureIndexEntryId:item.treasureIndexEntryId,
          magicalQualities:[...(item.magicalQualities||[])],
          magicalQualitiesUnlocked:item.magicalQualitiesUnlocked,
          magicalRewards:item.magicalRewards?[...item.magicalRewards]:undefined,
          magicalRewardsUnlocked:item.magicalRewardsUnlocked,
        }];
      }
    }
  }
  sheet.attributes.load=sheet.inventory.reduce((sum,item)=>sum+Number(item.load||item.weight||0)*Math.max(1,Number(item.quantity)||1),0);
  sheet.other.heirOrigin={
    predecessorCharacterId:predecessor.id,
    previousExperienceBudget:budget,
    inheritedFavouredSkill:familySkill,
    inheritedItemNames:inherited.map(item=>item.name),
  };
  sheet.other.creationTrace=[
    ...(sheet.other.creationTrace||[]),
    'Herdeiro de '+(predecessor.sheet.identity.name||predecessor.name)+'.',
    'Experiência Anterior herdada: '+budget+' pontos.',
    'Perícia Favorecida de herança familiar: '+familySkill+'.',
    inherited.length?'Heranças: '+inherited.map(item=>item.name).join(', ')+'.':'Sem item de Herança.',
  ];
  sheet.other.notes=(sheet.other.notes||'')+' Herdeiro criado pelas regras de Outras Aventuras.';
  heir.name=sheet.identity.name;
  heir.sheet=normalizeEquipmentMechanics(sheet);
  return heir;
}
