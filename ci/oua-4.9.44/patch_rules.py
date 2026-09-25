from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_rules.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Canonical Reward sequence helpers + validation.
# ---------------------------------------------------------------------------
rel="lib/rules/magicalTreasure.ts"
t=read(rel)
append=r"""

export type FamousRewardRef={kind:'common'|'enchanted';id:string};

export const COMMON_REWARDS=[
  {id:'Afiada',item:'weapon'},
  {id:'Ajustado',item:'armor-helm'},
  {id:'Fabricação Habilidosa',item:'armor-helm-shield'},
  {id:'Cruel',item:'weapon'},
  {id:'Atroz',item:'weapon'},
  {id:'Reforçado',item:'shield'},
] as const;

const ENHANCED_CONFLICTS:Record<string,string>={
  'Afiada':'superior-keen',
  'Ajustado':'ancient-close-fitting',
  'Fabricação Habilidosa':'ancient-cunning-make',
  'Cruel':'superior-grievous',
  'Atroz':'very-fell',
  'Reforçado':'superior-reinforced',
};

function baseItemKind(baseItemName:string):'weapon'|'armor'|'helm'|'shield' {
  const key=norm(baseItemName);
  if(/escudo|broquel|shield/.test(key)) return 'shield';
  if(/elmo|helm/.test(key)) return 'helm';
  if(/armadura|couro|malha|cota|colete|mail/.test(key)) return 'armor';
  return 'weapon';
}

function commonRewardAllowed(reward:string,baseItemName:string):boolean {
  const kind=baseItemKind(baseItemName);
  if(reward==='Afiada'||reward==='Cruel'||reward==='Atroz') return kind==='weapon';
  if(reward==='Ajustado') return kind==='armor'||kind==='helm';
  if(reward==='Fabricação Habilidosa') return kind==='armor'||kind==='helm'||kind==='shield';
  if(reward==='Reforçado') return kind==='shield';
  return false;
}

function enchantedRewardAllowed(id:string,origin:MagicalOrigin,baseItemName:string):boolean {
  const rule=qualityById(id);
  if(!rule) return false;
  if(!rule.origins.includes('Any')&&!rule.origins.includes(origin)) return false;
  const kind=baseItemKind(baseItemName);
  const item=norm(rule.item);
  if(item.includes('arcos')) return kind==='weapon'&&/arco|bow/.test(norm(baseItemName));
  if(item.includes('distancia')||item.includes('distância')) return kind==='weapon'&&/arco|bow/.test(norm(baseItemName));
  if(item.includes('corpo a corpo')) return kind==='weapon'&&!/arco|bow/.test(norm(baseItemName));
  if(item.includes('escudo')) return kind==='shield';
  if(item.includes('elmo')&&item.includes('armadura')) return kind==='armor'||kind==='helm';
  if(item.includes('armadura de malha')) return kind==='armor'&&/malha|cota|mail/.test(norm(baseItemName));
  if(item.includes('armadura, elmo ou escudo')) return kind==='armor'||kind==='helm'||kind==='shield';
  if(item.includes('qualquer arma')) return kind==='weapon';
  return true;
}

function enchantedRewardRequiresBaneForOrigin(id:string,origin:MagicalOrigin):boolean {
  const rule=qualityById(id);
  if(!rule) return false;
  if(rule.bane==='required') return true;
  if(rule.bane==='if-elven'&&origin==='Elven') return true;
  if(rule.bane==='if-numenorean'&&origin==='Numenorean') return true;
  return false;
}

export function validateFamousRewardSequence(
  rewards:FamousRewardRef[],
  origin:MagicalOrigin,
  baseItemName:string,
  banes:Bane[]=[],
):{valid:boolean;reason:string} {
  const list=(rewards||[]).slice(0,4);
  if(list.length<1||list.length>3) return {valid:false,reason:'Arma/Armadura Famosa exige de 1 a 3 Recompensas no total.'};
  if(!list.some(ref=>ref.kind==='enchanted')) return {valid:false,reason:'A sequência deve conter pelo menos uma Recompensa Encantada.'};
  const seen=new Set<string>();
  for(const ref of list){
    const key=ref.kind+':'+ref.id;
    if(seen.has(key)) return {valid:false,reason:'A mesma Recompensa não pode ser aplicada duas vezes ao item.'};
    seen.add(key);
    if(ref.kind==='common'){
      if(!COMMON_REWARDS.some(value=>value.id===ref.id)) return {valid:false,reason:'Recompensa comum desconhecida: '+ref.id+'.'};
      if(!commonRewardAllowed(ref.id,baseItemName)) return {valid:false,reason:ref.id+' não pode ser aplicada a '+baseItemName+'.'};
      const enhanced=ENHANCED_CONFLICTS[ref.id];
      if(enhanced&&list.some(value=>value.kind==='enchanted'&&value.id===enhanced)) return {valid:false,reason:ref.id+' não pode coexistir com sua versão Encantada aprimorada.'};
    }else{
      if(!enchantedRewardAllowed(ref.id,origin,baseItemName)) return {valid:false,reason:'Recompensa Encantada incompatível com Origem ou tipo de item: '+ref.id+'.'};
      const common=Object.entries(ENHANCED_CONFLICTS).find(([,enchanted])=>enchanted===ref.id)?.[0];
      if(common&&list.some(value=>value.kind==='common'&&value.id===common)) return {valid:false,reason:common+' não pode coexistir com sua versão Encantada aprimorada.'};
    }
  }
  if(baseItemKind(baseItemName)==='weapon'&&(origin==='Elven'||origin==='Numenorean')){
    const hasBaneQuality=list.some(ref=>ref.kind==='enchanted'&&enchantedRewardRequiresBaneForOrigin(ref.id,origin));
    if(!hasBaneQuality) return {valid:false,reason:'Arma Élfica ou Numenoriana exige pelo menos uma Recompensa Encantada de Flagelo.'};
    if(!banes.length) return {valid:false,reason:'A arma de Flagelo precisa de ao menos uma categoria de Flagelo compatível com a Origem.'};
  }
  return {valid:true,reason:'Sequência de Recompensas válida.'};
}

export function famousRewardSequence(item:CharacterSheet['inventory'][number]):FamousRewardRef[] {
  if(Array.isArray(item.magicalRewards)&&item.magicalRewards.length) return item.magicalRewards.map(ref=>({kind:ref.kind,id:String(ref.id)}));
  return (item.magicalQualities||[]).map(id=>({kind:'enchanted' as const,id:String(id)}));
}

export function activeFamousRewardSequence(item:CharacterSheet['inventory'][number]):FamousRewardRef[] {
  if(item.magicalCurse==='ownership'&&item.magicalCurseActive&&item.magicalCurseOwnerPresent) return [];
  const sequence=famousRewardSequence(item);
  const count=Math.max(0,Math.min(sequence.length,Math.trunc(Number(item.magicalRewardsUnlocked ?? item.magicalQualitiesUnlocked)||0)));
  return sequence.slice(0,count);
}

export function activeCommonRewardNames(item:CharacterSheet['inventory'][number]):string[] {
  return activeFamousRewardSequence(item).filter(ref=>ref.kind==='common').map(ref=>ref.id);
}

export function activeEnchantedRewardIds(item:CharacterSheet['inventory'][number]):string[] {
  return activeFamousRewardSequence(item).filter(ref=>ref.kind==='enchanted').map(ref=>ref.id);
}
"""
t+=append
write(rel,t)

# Famous enchanted mechanics now follow the unified ordered sequence.
rel="lib/rules/magicalGearMechanics.ts"
t=read(rel)
t=t.replace(
"import { activeQualityIds, type Bane } from './magicalTreasure';",
"import { activeEnchantedRewardIds, type Bane } from './magicalTreasure';",
1)
t=req(t,
"""export function activeQualitiesForItem(item:InventoryItem):string[] {
  if(item.magicalKind!=='famous-war-gear' || magicalItemSuppressed(item)) return [];
  return activeQualityIds(item.magicalQualities,item.magicalQualitiesUnlocked);
}""",
"""export function activeQualitiesForItem(item:InventoryItem):string[] {
  if(item.magicalKind!=='famous-war-gear' || magicalItemSuppressed(item)) return [];
  return activeEnchantedRewardIds(item);
}""",
"unified active enchanted rewards")
write(rel,t)

# Existing ordinary Reward engine recognizes awakened common qualities.
rel="lib/rules/equipment.ts"
t=read(rel)
if "activeCommonRewardNames" not in t:
    t=t.replace(
        "import { enchantedLoad, famousShieldStaticParryBonus } from './magicalGearMechanics';",
        "import { enchantedLoad, famousShieldStaticParryBonus } from './magicalGearMechanics';\nimport { activeCommonRewardNames } from './magicalTreasure';",
        1)
t=req(t,
"""function itemReward(sheet: CharacterSheet, item: CharacterSheet['inventory'][number], name: string): boolean {
  const n=norm(name);
  if(tagsOf(item).some(tag=>norm(tag)===('recompensa:'+n))) return true;""",
"""function itemReward(sheet: CharacterSheet, item: CharacterSheet['inventory'][number], name: string): boolean {
  const n=norm(name);
  if(item.magicalKind==='famous-war-gear'&&activeCommonRewardNames(item).some(value=>norm(value)===n)) return true;
  if(tagsOf(item).some(tag=>norm(tag)===('recompensa:'+n))) return true;""",
"awakened common Reward effects")
write(rel,t)

# Progression awakens the next entry in the unified Reward sequence.
rel="lib/rules/progression.ts"
t=read(rel)
t=req(t,
"""  const total=(item.magicalQualities||[]).length;
  const unlocked=Math.max(0,Math.trunc(Number(item.magicalQualitiesUnlocked)||0));
  if(unlocked>=total) return {sheet:input,cost,allowed:false,reason:'Todas as qualidades desta peça já estão ativas.'};
  next.resources.adventurePoints-=cost;
  next.attributes.valor=target;
  item.magicalQualitiesUnlocked=Math.min(total,unlocked+1);
  next.combat.weapons=(next.combat.weapons||[]).map(weapon=>weapon.treasureIndexEntryId===item.treasureIndexEntryId?{...weapon,magicalQualitiesUnlocked:item.magicalQualitiesUnlocked}:weapon);""",
"""  const total=(item.magicalRewards||[]).length || (item.magicalQualities||[]).length;
  const unlocked=Math.max(0,Math.trunc(Number(item.magicalRewardsUnlocked ?? item.magicalQualitiesUnlocked)||0));
  if(unlocked>=total) return {sheet:input,cost,allowed:false,reason:'Todas as qualidades desta peça já estão ativas.'};
  next.resources.adventurePoints-=cost;
  next.attributes.valor=target;
  item.magicalRewardsUnlocked=Math.min(total,unlocked+1);
  if(!item.magicalRewards?.length) item.magicalQualitiesUnlocked=item.magicalRewardsUnlocked;
  next.combat.weapons=(next.combat.weapons||[]).map(weapon=>weapon.treasureIndexEntryId===item.treasureIndexEntryId?{
    ...weapon,
    magicalRewardsUnlocked:item.magicalRewardsUnlocked,
    magicalQualitiesUnlocked:item.magicalRewards?.length?weapon.magicalQualitiesUnlocked:item.magicalQualitiesUnlocked,
  }:weapon);""",
"Valor mixed Reward awakening")
write(rel,t)

print("PATCH_4944_RULES_OK")
