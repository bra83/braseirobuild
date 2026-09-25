from pathlib import Path
import sys

if len(sys.argv)!=2: raise SystemExit("usage: patch_creation.py <project-root>")
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
    "function spendPreviousExperience(ranks:Record<string,number>, prof:Record<string,number>):string[]{",
    "function spendPreviousExperience(ranks:Record<string,number>, prof:Record<string,number>, budget=10):string[]{",
    "xp budget signature",
)
t=req(
    t,
    "  if(!solve(10))throw new Error('Criação inválida: não foi possível distribuir exatamente os 10 pontos de Experiência Prévia.');",
    "  if(!solve(budget))throw new Error('Criação inválida: não foi possível distribuir exatamente '+budget+' pontos de Experiência Prévia.');",
    "xp solve budget",
)
t=req(
    t,
    "  const distinctive=s.other.distinctiveFeatures||[]; if(distinctive.length!==2||distinctive.some(x=>!c.features.includes(x)))issues.push('Características Distintivas não pertencem à Cultura.');",
    """  const distinctive=s.other.distinctiveFeatures||[];
  const strider=s.other.rulesMode==='strider';
  const culturalDistinctive=distinctive.filter(x=>x!=='Peregrino');
  if(culturalDistinctive.length!==2||culturalDistinctive.some(x=>!c.features.includes(x)))issues.push('Características Distintivas não pertencem à Cultura.');
  if(strider&&!distinctive.includes('Peregrino'))issues.push('Modo Peregrino exige a Característica Notável extra Peregrino.');
  if(!strider&&distinctive.includes('Peregrino'))issues.push('Peregrino só deve ser aplicado pelo Modo Peregrino.');""",
    "strider distinctive validation",
)
t=req(
    t,
    "  if(!legalXp)issues.push('A distribuição final não corresponde a exatamente 10 pontos legais de Experiência Prévia.');",
    """  const expectedXp=s.other.rulesMode==='strider'?15:10;
  if(!legalXp&&expectedXp===10)issues.push('A distribuição final não corresponde a exatamente 10 pontos legais de Experiência Prévia.');
  if(expectedXp===15){
    legalXp=false;
    for(const primary of c.prof2){for(const secondary of groups.filter(g=>g!==primary)){const initial:Record<Proficiency,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0};initial[primary]=2;initial[secondary]=1;let cost=skillXp,possible=true;for(const g of groups){const target=Number(finalProf[g]||0);if(target<initial[g]){possible=false;break;}cost+=profCostForTarget(initial[g],target);}if(possible&&cost===15)legalXp=true;}}
    if(!legalXp)issues.push('A distribuição final do Modo Peregrino não corresponde a exatamente 15 pontos legais de Experiência Prévia.');
  }""",
    "strider xp validation",
)
t=req(
    t,
    "export function createRandomCharacter(existingNames:string[]=[]):Character{",
    "export function createRandomCharacter(existingNames:string[]=[], options:{rulesMode?:'standard'|'strider'}={}):Character{",
    "creation options",
)
t=req(
    t,
    "  const base=pick(culture.attributes); let [strength,heart,wits]=base;",
    """  const rulesMode=options.rulesMode==='strider'?'strider':'standard';
  const base=pick(culture.attributes); let [strength,heart,wits]=base;""",
    "creation rules mode",
)
t=req(
    t,
    "  trace.push(...spendPreviousExperience(ranks,prof));",
    """  const previousExperienceBudget=rulesMode==='strider'?15:10;
  trace.push(...spendPreviousExperience(ranks,prof,previousExperienceBudget));
  trace.push('Experiência Prévia: '+previousExperienceBudget+'/'+previousExperienceBudget+' pontos.');""",
    "creation xp mode",
)
t=req(
    t,
    "  const reward=pick(REWARDS), features=sample(culture.features,2), name=uniqueName(pick(culture.names),existingNames);",
    """  const reward=pick(REWARDS), features=sample(culture.features,2), name=uniqueName(pick(culture.names),existingNames);
  if(rulesMode==='strider') features.push('Peregrino');""",
    "strider feature",
)
old="notes:\`Herói criado automaticamente por fontes 2e autorizadas (\${sourceForCulture(culture.culture)}). Experiência Prévia: 10/10 pontos. Patrono e Refúgio Seguro ficam para a formação da Companhia.\`,narrativeConditions:'Sem Ferimento; Sombra 0; Fadiga 0.',culturalBlessing:culture.blessing,shadowPath:calling.shadow,callingFeature:calling.feature,distinctiveFeatures:features,creationTrace:trace}"
new="notes:\`Herói criado automaticamente por fontes 2e autorizadas (\${sourceForCulture(culture.culture)}). Experiência Prévia: \${previousExperienceBudget}/\${previousExperienceBudget} pontos. Patrono e Refúgio Seguro ficam para a formação da Companhia.\`,narrativeConditions:'Sem Ferimento; Sombra 0; Fadiga 0.',culturalBlessing:culture.blessing,shadowPath:calling.shadow,callingFeature:calling.feature,distinctiveFeatures:features,creationTrace:trace,rulesMode}"
t=req(t,old,new,"creation rules mode metadata")
write(rel,t)

print("PATCH_4939_CREATION_OK")
