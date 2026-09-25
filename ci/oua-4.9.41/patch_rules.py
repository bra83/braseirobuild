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
# Shadow: expose canonical first-Flaw sequence for Character Paths.
# ---------------------------------------------------------------------------
rel="lib/rules/shadow.ts"
t=read(rel)
t=req(t,
"export function recomputeShadowState(input: CharacterSheet): CharacterSheet {",
"""export function flawsForShadowPath(shadowPath:string): string[] {
  return [...(FLAWS[norm(shadowPath)]||[])];
}

export function firstFlawForShadowPath(shadowPath:string): string | null {
  return flawsForShadowPath(shadowPath)[0] || null;
}

export function recomputeShadowState(input: CharacterSheet): CharacterSheet {""",
"export shadow flaws")
write(rel,t)

# ---------------------------------------------------------------------------
# Progression: Valor may awaken next Famous quality instead of taking Reward.
# ---------------------------------------------------------------------------
rel="lib/rules/progression.ts"
t=read(rel)
insert="""export function advanceValorForFamousQuality(
  input:CharacterSheet,
  inventoryItemId:string,
):{sheet:CharacterSheet;cost:number;allowed:boolean;reason:string}{
  const next=structuredClone(input);
  const target=Math.max(2,Number(next.attributes.valor||1)+1);
  if(target>6) return {sheet:input,cost:0,allowed:false,reason:'Valor já está no nível máximo 6.'};
  const cost=costFor(target);
  if(next.resources.adventurePoints<cost) return {sheet:input,cost,allowed:false,reason:'Pontos de Aventura insuficientes.'};
  const item=next.inventory.find(entry=>entry.id===inventoryItemId);
  if(!item||item.magicalKind!=='famous-war-gear') return {sheet:input,cost,allowed:false,reason:'Selecione uma Arma ou Armadura Famosa.'};
  const total=(item.magicalQualities||[]).length;
  const unlocked=Math.max(0,Math.trunc(Number(item.magicalQualitiesUnlocked)||0));
  if(unlocked>=total) return {sheet:input,cost,allowed:false,reason:'Todas as qualidades desta peça já estão ativas.'};
  next.resources.adventurePoints-=cost;
  next.attributes.valor=target;
  item.magicalQualitiesUnlocked=Math.min(total,unlocked+1);
  next.combat.weapons=(next.combat.weapons||[]).map(weapon=>weapon.treasureIndexEntryId===item.treasureIndexEntryId?{...weapon,magicalQualitiesUnlocked:item.magicalQualitiesUnlocked}:weapon);
  return {sheet:normalizeEquipmentMechanics(next),cost,allowed:true,reason:'Valor elevado para '+target+'; a próxima qualidade da peça famosa foi ativada em vez de uma nova Recompensa.'};
}

"""
t=req(t,"export function advanceWisdom(",insert+"export function advanceWisdom(","Valor famous quality")
write(rel,t)

# ---------------------------------------------------------------------------
# Equipment: a Famous item keeps its unique name while inheriting mechanics
# from the mundane base item recorded in magicalBaseItemName.
# ---------------------------------------------------------------------------
rel="lib/rules/equipment.ts"
t=read(rel)
t=req(t,
"function canonicalLoad(sheet: CharacterSheet,item: CharacterSheet['inventory'][number]): number {\n  const n=norm(item.name);",
"function canonicalLoad(sheet: CharacterSheet,item: CharacterSheet['inventory'][number]): number {\n  const n=norm(item.magicalBaseItemName||item.name);",
"magical base load")
t=req(t,
"function canonicalProtection(item: CharacterSheet['inventory'][number]): number {\n  const n=norm(item.name);",
"function canonicalProtection(item: CharacterSheet['inventory'][number]): number {\n  const n=norm(item.magicalBaseItemName||item.name);",
"magical base protection")
t=req(t,
"function shieldParry(sheet: CharacterSheet,item: CharacterSheet['inventory'][number]): number {\n  const base=SHIELDS[norm(item.name)]?.parry ?? 0;",
"function shieldParry(sheet: CharacterSheet,item: CharacterSheet['inventory'][number]): number {\n  const base=SHIELDS[norm(item.magicalBaseItemName||item.name)]?.parry ?? 0;",
"magical base shield")
t=req(t,
"""  next.combat.weapons=(next.combat.weapons||[]).map(weapon=>{
    const item=next.inventory.find(x=>norm(x.name)===norm(weapon.name));
    const base=canonicalWeapon(weapon.name);""",
"""  next.combat.weapons=(next.combat.weapons||[]).map(weapon=>{
    const item=next.inventory.find(x=>x.treasureIndexEntryId&&x.treasureIndexEntryId===weapon.treasureIndexEntryId)
      || next.inventory.find(x=>norm(x.name)===norm(weapon.name));
    const base=canonicalWeapon(weapon.magicalBaseItemName||item?.magicalBaseItemName||weapon.name);""",
"magical named weapon normalization")
write(rel,t)

# ---------------------------------------------------------------------------
# Magical Treasure runtime helpers: Blessings, identified Magical Success,
# active qualities, curse flags.
# ---------------------------------------------------------------------------
rel="lib/rules/magicalTreasure.ts"
t=read(rel)
t="import type { CharacterSheet } from '../db';\n"+t
append="""

function norm(value:unknown):string {
  return String(value??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
}

export function magicalBlessingDice(sheet:CharacterSheet,skill:string):number {
  const key=norm(skill);
  return (sheet.inventory||[]).some(item=>
    ['magnificent-artefact','wondrous-item'].includes(String(item.magicalKind||''))
    && (item.magicalBlessings||[]).some(blessing=>norm(blessing)===key)
  ) ? 2 : 0;
}

export function identifiedBlessingCanMagicalSuccess(sheet:CharacterSheet,skill:string):boolean {
  const key=norm(skill);
  return (sheet.inventory||[]).some(item=>
    ['magnificent-artefact','wondrous-item'].includes(String(item.magicalKind||''))
    && Boolean(item.magicalIdentified)
    && (item.magicalBlessings||[]).some(blessing=>norm(blessing)===key)
  );
}

export function blessingSource(sheet:CharacterSheet,skill:string):string|null {
  const key=norm(skill);
  const item=(sheet.inventory||[]).find(entry=>
    ['magnificent-artefact','wondrous-item'].includes(String(entry.magicalKind||''))
    && Boolean(entry.magicalIdentified)
    && (entry.magicalBlessings||[]).some(blessing=>norm(blessing)===key)
  );
  return item ? 'Bênção · '+item.name : null;
}

export function activeFamousQualities(item:CharacterSheet['inventory'][number]):string[] {
  if(item.magicalKind!=='famous-war-gear') return [];
  return activeQualityIds(item.magicalQualities,item.magicalQualitiesUnlocked);
}

export function hasActiveFamousQuality(sheet:CharacterSheet,qualityId:string,weaponName?:string):boolean {
  return (sheet.inventory||[]).some(item=>{
    if(item.magicalKind!=='famous-war-gear') return false;
    if(weaponName && norm(item.name)!==norm(weaponName) && norm(item.magicalBaseItemName)!==norm(weaponName)) return false;
    return activeFamousQualities(item).includes(qualityId);
  });
}

export function activeMagicalCurseItems(sheet:CharacterSheet):CharacterSheet['inventory'] {
  return (sheet.inventory||[]).filter(item=>Boolean(item.magicalCurseActive&&item.magicalCurse));
}

export function curseBlocksHopeForSkill(sheet:CharacterSheet,skill:string):boolean {
  const key=norm(skill);
  return activeMagicalCurseItems(sheet).some(item=>{
    if(item.magicalCurse!=='malice') return false;
    return (item.magicalBlessings||[]).some(blessing=>norm(blessing)===key);
  });
}

export function illOmenCouncilPenalty(sheet:CharacterSheet):number {
  return activeMagicalCurseItems(sheet).some(item=>item.magicalCurse==='ill-omen') ? -1 : 0;
}

export function curseTnModifier(sheet:CharacterSheet,attribute:'strength'|'heart'|'wits'):number {
  return activeMagicalCurseItems(sheet).reduce((sum,item)=>{
    if(item.magicalCurse!=='weakening') return sum;
    const tag=(item.tags||[]).find(value=>norm(value).startsWith('curse-attribute:'));
    const chosen=tag?norm(tag.split(':')[1]):'';
    return sum+(chosen===attribute?2:0);
  },0);
}

export function shadowTaintFloor(sheet:CharacterSheet):number {
  return activeMagicalCurseItems(sheet).reduce((sum,item)=>{
    if(item.magicalCurse!=='shadow-taint') return sum;
    if(item.magicalKind==='magnificent-artefact') return sum+1;
    if(item.magicalKind==='wondrous-item') return sum+2;
    if(item.magicalKind==='famous-war-gear'){
      const active=activeFamousQualities(item)
        .map(id=>qualityById(id))
        .filter(Boolean).length;
      return sum+2*active;
    }
    return sum;
  },0);
}
"""
t+=append
write(rel,t)

# Character TN includes active Weakening curse.
rel="lib/characterRules.ts"
t=read(rel)
if "curseTnModifier" not in t:
    t=t.replace(
        "import type { CharacterSheet } from './db';",
        "import type { CharacterSheet } from './db';\nimport { curseTnModifier } from './rules/magicalTreasure';",
        1)
t=req(t,
"  const key = attributeForSkill(skillName); const attribute = Math.max(1, Number(sheet.attributes[key]) || 0); const modifier = Number(sheet.attributes.tnModifiers?.[key] || 0); const base = sheet.other.rulesMode === 'strider' ? 18 : 20; return Math.max(6, Math.min(19, base - attribute + modifier));",
"  const key = attributeForSkill(skillName); const attribute = Math.max(1, Number(sheet.attributes[key]) || 0); const modifier = Number(sheet.attributes.tnModifiers?.[key] || 0) + curseTnModifier(sheet,key); const base = sheet.other.rulesMode === 'strider' ? 18 : 20; return Math.max(6, Math.min(21, base - attribute + modifier));",
"curse TN")
write(rel,t)

print("PATCH_4941_RULES_OK")
