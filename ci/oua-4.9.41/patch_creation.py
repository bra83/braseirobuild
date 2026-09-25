from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_creation.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="lib/characterFactory.ts"
t=read(rel)
t=req(
    t,
    "import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';",
    "import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';\nimport { backgroundById, importantEventByFeat, shiftLifestyle, type CharacterPathCultureId } from './characterPaths';\nimport { firstFlawForShadowPath } from './rules/shadow';",
    "Character Paths imports",
)

append=r"""

export interface CharacterPathCreationOptions {
  cultureId: CharacterPathCultureId;
  backgroundId: string;
  importantEventFeat: number;
  existingNames?: string[];
  callingName?: string;
  rulesMode?: 'standard'|'strider';
  tnImprovementAttribute?: 'strength'|'heart'|'wits';
  addedFavouredSkills?: string[];
  removedFavouredSkills?: string[];
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
    const target=Math.floor(Math.random()*3);
    if(target===0) strength++; else if(target===1) heart++; else wits++;
    trace.push('Reis dos Homens: +1 em '+['Força','Coração','Astúcia'][target]+'.');
  }

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
  const callingFav=sample(callingPool.length>=2?callingPool:calling.favoured,2);
  const favourite=new Set<string>([background.favouredSkill,...callingFav]);

  const virtue=pick(VIRTUES);
  if(virtue==='Maestria'){
    for(const skill of sample(CANONICAL_SKILLS.filter(name=>!favourite.has(name)),2)) favourite.add(skill);
  }

  const requestedAdds=Array.from(new Set((options.addedFavouredSkills||[]).map(String)));
  const requestedRemovals=Array.from(new Set((options.removedFavouredSkills||[]).map(String)));
  if(event.addFavouredSkills){
    if(requestedAdds.length!==event.addFavouredSkills) throw new Error('O Evento '+event.title+' exige escolher '+event.addFavouredSkills+' Perícia adicional para ficar Favorecida.');
    for(const skill of requestedAdds){
      if(!CANONICAL_SKILLS.includes(skill as SkillName)) throw new Error('Perícia adicional inválida: '+skill);
      favourite.add(skill);
    }
  } else if(requestedAdds.length) throw new Error('Este Evento não permite adicionar Perícias Favorecidas.');

  if(event.removeFavouredSkills){
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
  if(virtue==='Resiliência') endurance+=2;
  if(virtue==='Destreza') parry+=1;
  if(virtue==='Dom'){
    const key=pick(['strength','heart','wits'] as const);
    tnModifiers[key]=(tnModifiers[key]||0)-1;
    trace.push('Dom: NA de '+key+' reduzido em 1.');
  }

  if(event.fixedTnModifier){
    tnModifiers[event.fixedTnModifier.attribute]=(tnModifiers[event.fixedTnModifier.attribute]||0)+event.fixedTnModifier.amount;
  }
  if(event.chooseTnImprovement){
    const penalized=event.fixedTnModifier?.attribute;
    const chosen=options.tnImprovementAttribute;
    if(!chosen||chosen===penalized) throw new Error(event.title+' exige escolher outro NA de Atributo para diminuir em 1.');
    tnModifiers[chosen]=(tnModifiers[chosen]||0)-1;
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

  const weapons=profEntries.map(([group],index)=>{
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
"""
t+=append
write(rel,t)

print("PATCH_4941_CREATION_OK")
