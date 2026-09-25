from pathlib import Path
import shutil, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_core.py <project-root>")
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text:
        raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

for src_rel,dst_rel in [
    ("templates/lib/supplementRules.ts","lib/supplementRules.ts"),
    ("templates/lib/rules/supplementMechanics.ts","lib/rules/supplementMechanics.ts"),
    ("templates/lib/rules/striderMode.ts","lib/rules/striderMode.ts"),
]:
    src=here/src_rel
    dst=root/dst_rel
    dst.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(src,dst)

rel="lib/db.ts"
t=read(rel)
t=req(t,
"""    creationTrace?: string[];
  };""",
"""    creationTrace?: string[];
    rulesMode?: 'standard' | 'strider';
    supplementSelections?: {
      beornEnchantedSkills?: string[];
      houndAvailable?: boolean;
      [key: string]: unknown;
    };
  };""","character supplement metadata")
write(rel,t)

rel="lib/sourceAuthority.ts"
t=read(rel)
if "15r-r6SFx3RWnRTlI7i-S4JGYp4zGYdH1" not in t:
    t=req(t,
"  ['15wjw3LYORJeBvG2OB3w4SwbgBl6c-xto','supplement-2e'], // Povos das Terras Ermas",
"  ['15wjw3LYORJeBvG2OB3w4SwbgBl6c-xto','supplement-2e'], // Povos das Terras Ermas\n  ['15r-r6SFx3RWnRTlI7i-S4JGYp4zGYdH1','supplement-2e'], // Povos das Terras-Selváticas (tradução alternativa)",
"alternate Wilderland source")
t=t.replace("return a==='core-2e'||a==='supplement-2e';","return a==='core-2e'||a==='supplement-2e'||a==='solo-2e';")
write(rel,t)

rel="lib/characterRules.ts"
t=read(rel)
t=req(t,
"""export function targetNumberForSkill(sheet: CharacterSheet, skillName: string): number {
  const key = attributeForSkill(skillName); const attribute = Math.max(1, Number(sheet.attributes[key]) || 0); const modifier = Number(sheet.attributes.tnModifiers?.[key] || 0); return Math.max(8, Math.min(19, 20 - attribute + modifier));
}""",
"""export function targetNumberForSkill(sheet: CharacterSheet, skillName: string): number {
  const key = attributeForSkill(skillName);
  const attribute = Math.max(1, Number(sheet.attributes[key]) || 0);
  const modifier = Number(sheet.attributes.tnModifiers?.[key] || 0);
  const base = sheet.other.rulesMode === 'strider' ? 18 : 20;
  return Math.max(6, Math.min(19, base - attribute + modifier));
}""","Strider target number")
write(rel,t)

rel="lib/characterFactory.ts"
t=read(rel)
t=t.replace("import { CANONICAL_SKILLS } from './skillRegistry';","import { CANONICAL_SKILLS, attributeForCanonicalSkill } from './skillRegistry';")
t=t.replace("import { SUPPLEMENT_CULTURES, sourceForCulture } from './supplementRules';","import { SUPPLEMENT_CULTURES, sourceForCulture, virtuesForCulture } from './supplementRules';")
t=req(t,
"function spendPreviousExperience(ranks:Record<string,number>, prof:Record<string,number>):string[]{",
"function spendPreviousExperience(ranks:Record<string,number>, prof:Record<string,number>, totalBudget=10):string[]{",
"experience helper signature")
t=t.replace("if(!solve(10))throw new Error('Criação inválida: não foi possível distribuir exatamente os 10 pontos de Experiência Prévia.');",
"if(!solve(totalBudget))throw new Error('Criação inválida: não foi possível distribuir exatamente '+String(totalBudget)+' pontos de Experiência Prévia.');")

t=req(t,
"""  const distinctive=s.other.distinctiveFeatures||[]; if(distinctive.length!==2||distinctive.some(x=>!c.features.includes(x)))issues.push('Características Distintivas não pertencem à Cultura.');""",
"""  const striderMode=s.other.rulesMode==='strider';
  const distinctive=s.other.distinctiveFeatures||[];
  const culturalDistinctive=distinctive.filter(x=>x!=='Peregrino');
  if(culturalDistinctive.length!==2||culturalDistinctive.some(x=>!c.features.includes(x)))issues.push('Características Distintivas não pertencem à Cultura.');
  if(striderMode&&!distinctive.includes('Peregrino'))issues.push('Modo Peregrino exige a Característica Notável Peregrino.');
  if(!striderMode&&distinctive.includes('Peregrino'))issues.push('Peregrino só deve ser aplicado no Modo Peregrino.');""","Strider distinctive validation")

t=req(t,
"""  const expectedEndurance=s.attributes.strength+c.derived[0]+(s.other.virtues.includes('Resiliência')?2:0); if(s.attributes.enduranceMax!==expectedEndurance||s.attributes.enduranceCurrent!==expectedEndurance)issues.push('Resistência inicial não deriva da Cultura/Virtude.');
  const expectedHope=s.attributes.heart+c.derived[1]+(s.other.virtues.includes('Confiança')?2:0); if(s.attributes.hopeMax!==expectedHope||s.attributes.hopeCurrent!==expectedHope)issues.push('Esperança inicial não deriva da Cultura/Virtude.');""",
"""  const expectedEndurance=s.attributes.strength+c.derived[0]+(s.other.virtues.includes('Resiliência')?2:0)+(s.other.virtues.includes('Irmão para os Ursos')?1:0); if(s.attributes.enduranceMax!==expectedEndurance||s.attributes.enduranceCurrent!==expectedEndurance)issues.push('Resistência inicial não deriva da Cultura/Virtude.');
  const expectedHope=s.attributes.heart+c.derived[1]+(s.other.virtues.includes('Confiança')?2:0)+(s.other.virtues.includes('Beleza das Estrelas')?1:0)+(s.other.virtues.includes('Música de Estancamento')?1:0); if(s.attributes.hopeMax!==expectedHope||s.attributes.hopeCurrent!==expectedHope)issues.push('Esperança inicial não deriva da Cultura/Virtude.');""","supplement max stats validation")

t=req(t,
"""  if(!legalXp)issues.push('A distribuição final não corresponde a exatamente 10 pontos legais de Experiência Prévia.');""",
"""  const expectedPreviousExperience=striderMode?15:10;
  if(expectedPreviousExperience===10&&!legalXp)issues.push('A distribuição final não corresponde a exatamente 10 pontos legais de Experiência Prévia.');
  if(expectedPreviousExperience===15){
    let legalStriderXp=false;
    for(const primary of c.prof2){for(const secondary of groups.filter(g=>g!==primary)){const initial:Record<Proficiency,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0};initial[primary]=2;initial[secondary]=1;let cost=skillXp,possible=true;for(const g of groups){const target=Number(finalProf[g]||0);if(target<initial[g]){possible=false;break;}cost+=profCostForTarget(initial[g],target);}if(possible&&cost===15)legalStriderXp=true;}}
    if(!legalStriderXp)issues.push('Modo Peregrino exige exatamente 15 pontos legais de Experiência Prévia.');
  }""","Strider XP validation")

t=req(t,
"export function createRandomCharacter(existingNames:string[]=[]):Character{",
"export function createRandomCharacter(existingNames:string[]=[], options:{rulesMode?:'standard'|'strider'}={}):Character{",
"create character options")

t=req(t,
"  const culture=pick(CULTURES), calling=pick(CALLINGS),",
"""  const rulesMode=options.rulesMode==='strider'?'strider':'standard';
  const previousExperience=rulesMode==='strider'?15:10;
  const culture=pick(CULTURES), calling=pick(CALLINGS),""",
"creation mode variables")

t=t.replace("trace.push(...spendPreviousExperience(ranks,prof));","trace.push(...spendPreviousExperience(ranks,prof,previousExperience));")

t=req(t,
"  const reward=pick(REWARDS), features=sample(culture.features,2), name=uniqueName(pick(culture.names),existingNames);",
"""  const reward=pick(REWARDS), features=sample(culture.features,2), name=uniqueName(pick(culture.names),existingNames);
  if(rulesMode==='strider') features.push('Peregrino');""",
"Peregrino feature")

dynamic = chr(36)+"{previousExperience}/"+chr(36)+"{previousExperience}"
t=t.replace("Experiência Prévia: 10/10 pontos.","Experiência Prévia: "+dynamic+" pontos.")
t=t.replace("creationTrace:trace},tokenId:null","creationTrace:trace,rulesMode,supplementSelections:{houndAvailable:virtue==='Cão da Floresta das Trevas',beornEnchantedSkills:virtue==='O Encantamento de Beorn'?sample(CANONICAL_SKILLS.filter(skill=>attributeForCanonicalSkill(skill)==='strength'),3):undefined}},tokenId:null")
write(rel,t)

print("PATCH_4939_CORE_OK")
