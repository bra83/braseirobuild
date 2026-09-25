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

# Allow the canonical generator to spend an Heir's accumulated Previous
# Experience pool instead of the normal 10/Strider 15.
t=req(
    t,
    "export function createRandomCharacter(existingNames:string[]=[], options:{rulesMode?:'standard'|'strider'}={}):Character{",
    "export function createRandomCharacter(existingNames:string[]=[], options:{rulesMode?:'standard'|'strider';previousExperienceBudget?:number}={}):Character{",
    "heir creation budget option",
)
t=req(
    t,
    "  const previousExperienceBudget=rulesMode==='strider'?15:10;",
    "  const previousExperienceBudget=options.previousExperienceBudget!==undefined?Math.max(0,Math.min(20,Math.trunc(options.previousExperienceBudget))):(rulesMode==='strider'?15:10);",
    "heir previous Experience budget",
)
t=t.replace(
    "distinctiveFeatures:features,creationTrace:trace,rulesMode}",
    "distinctiveFeatures:features,creationTrace:trace,rulesMode,creationPreviousExperienceBudget:previousExperienceBudget}",
    1,
)

# Creation validator: an Heir may legally have 10–20 Previous Experience.
# The chosen budget is persisted in heirOrigin and checked against the same
# skill/proficiency cost reconstruction.
t=req(
    t,
    "  const expectedXp=s.other.rulesMode==='strider'?15:10;",
    "  const expectedXp=Math.max(0,Math.min(20,Math.trunc(Number(s.other.heirOrigin?.previousExperienceBudget ?? s.other.creationPreviousExperienceBudget ?? (s.other.rulesMode==='strider'?15:10)))));",
    "heir validation budget",
)
t=req(
    t,
    """  if(!legalXp&&expectedXp===10)issues.push('A distribuição final não corresponde a exatamente 10 pontos legais de Experiência Prévia.');
  if(expectedXp===15){
    legalXp=false;
    for(const primary of c.prof2){for(const secondary of groups.filter(g=>g!==primary)){const initial:Record<Proficiency,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0};initial[primary]=2;initial[secondary]=1;let cost=skillXp,possible=true;for(const g of groups){const target=Number(finalProf[g]||0);if(target<initial[g]){possible=false;break;}cost+=profCostForTarget(initial[g],target);}if(possible&&cost===15)legalXp=true;}}
    if(!legalXp)issues.push('A distribuição final do Modo Peregrino não corresponde a exatamente 15 pontos legais de Experiência Prévia.');
  }""",
    """  if(!legalXp || expectedXp!==10){
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
  if(!legalXp)issues.push('A distribuição final não corresponde a exatamente '+expectedXp+' pontos legais de Experiência Prévia.');""",
    "generic Previous Experience validation",
)

# Standard cultural lifestyle is overridden only for a documented Heir origin.
t=req(
    t,
    "  if(s.identity.lifestyle!==c.lifestyle)issues.push('Padrão de Vida não corresponde à Cultura.');",
    "  if(!s.other.heirOrigin&&s.identity.lifestyle!==c.lifestyle)issues.push('Padrão de Vida não corresponde à Cultura.');",
    "heir inherited lifestyle validation",
)

# Add a focused helper that creates a normal character with the predecessor's
# Previous Experience budget/lifestyle and explicit family Favoured Skill.
append=r"""

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
"""
# Need normalizeEquipmentMechanics import in this module.
if "normalizeEquipmentMechanics" not in t.split("\n",20)[0:20]:
    pass
if "from './rules/equipment'" not in t:
    t=t.replace(
        "import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';",
        "import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';\nimport { normalizeEquipmentMechanics } from './rules/equipment';",
        1,
    )
t += append
write(rel,t)

# Persist the origin metadata on the heir sheet.
rel="lib/db.ts"
t=read(rel)
t=req(t,
"""    heirState?: {
      name?: string;""",
"""    creationPreviousExperienceBudget?: number;
    heirOrigin?: {
      predecessorCharacterId: string;
      previousExperienceBudget: number;
      inheritedFavouredSkill: string;
      inheritedItemNames?: string[];
    };
    heirState?: {
      name?: string;""",
"heir origin metadata")
write(rel,t)

print("PATCH_4946_CREATION_OK")
