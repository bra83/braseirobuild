from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_virtues.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text:
        raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Progression: supplement Virtues must apply permanent derived-stat effects
# and cultural restrictions at acquisition time.
# ---------------------------------------------------------------------------
rel="lib/rules/progression.ts"
t=read(rel)
t=req(
    t,
    "import { normalizeEquipmentMechanics } from './equipment';",
    "import { normalizeEquipmentMechanics } from './equipment';\nimport { supplementMaxEnduranceBonus, supplementMaxHopeBonus } from './supplementMechanics';\nimport { supplementCultureFor, supplementVirtueByName } from '../supplementRules';",
    "progression supplement imports",
)
t=req(
    t,
    """export function advanceWisdom(input:CharacterSheet,virtue:string):{sheet:CharacterSheet;cost:number;allowed:boolean;reason:string}{
  const next=structuredClone(input); const target=Math.max(2,Number(next.attributes.wisdom||1)+1);""",
    """export function advanceWisdom(input:CharacterSheet,virtue:string):{sheet:CharacterSheet;cost:number;allowed:boolean;reason:string}{
  const next=structuredClone(input); const target=Math.max(2,Number(next.attributes.wisdom||1)+1);
  const supplementVirtue=supplementVirtueByName(virtue);
  if(supplementVirtue){
    const culture=supplementCultureFor(input.identity.culture);
    if(!culture || !supplementVirtue.cultureIds.includes(culture.id)) return {sheet:input,cost:0,allowed:false,reason:'Esta Virtude não pertence à Cultura deste Herói.'};
  }
  const oldHopeBonus=supplementMaxHopeBonus(input);
  const oldEnduranceBonus=supplementMaxEnduranceBonus(input);""",
    "progression cultural restriction",
)
t=req(
    t,
    """  if(v==='destreza') next.combat.parry+=1;
  return {sheet:normalizeEquipmentMechanics(next),cost,allowed:true,reason:'Sabedoria elevada para '+target+' e Virtude adquirida.'};""",
    """  if(v==='destreza') next.combat.parry+=1;
  const newHopeBonus=supplementMaxHopeBonus(next);
  const newEnduranceBonus=supplementMaxEnduranceBonus(next);
  const hopeDelta=Math.max(0,newHopeBonus-oldHopeBonus);
  const enduranceDelta=Math.max(0,newEnduranceBonus-oldEnduranceBonus);
  if(hopeDelta>0){next.attributes.hopeMax+=hopeDelta;next.attributes.hopeCurrent=Math.min(next.attributes.hopeMax,next.attributes.hopeCurrent+hopeDelta);}
  if(enduranceDelta>0){next.attributes.enduranceMax+=enduranceDelta;next.attributes.enduranceCurrent=Math.min(next.attributes.enduranceMax,next.attributes.enduranceCurrent+enduranceDelta);}
  return {sheet:normalizeEquipmentMechanics(next),cost,allowed:true,reason:'Sabedoria elevada para '+target+' e Virtude adquirida.'};""",
    "progression permanent supplement effects",
)
write(rel,t)

# ---------------------------------------------------------------------------
# Journey role validator: with 4+ heroes each covered role is normally held by
# a distinct hero. Vigilância Natural alone permits Lookout + another role.
# With fewer than 4, the core rule already permits multiple roles.
# ---------------------------------------------------------------------------
rel="lib/rules/journey.ts"
t=read(rel)
old="""export function validateJourneyRoles(roles:Partial<Record<JourneyRole,string>>,partyIds:string[]):string[]{
  const errors:string[]=[]; const party=new Set(partyIds); const required:JourneyRole[]=['guide','scout','hunter','lookout'];
  for(const role of required){
    const id=roles[role];
    if(!id) errors.push('A Companhia precisa definir '+JOURNEY_ROLE_LABELS[role]+' ('+JOURNEY_ROLE_SKILLS[role]+').');
    else if(!party.has(id)) errors.push(JOURNEY_ROLE_LABELS[role]+': personagem fora da Companhia.');
  }
  return errors;
}"""
new="""export function validateJourneyRoles(
  roles:Partial<Record<JourneyRole,string>>,
  partyIds:string[],
  naturalWatchfulnessIds:string[]=[],
):string[]{
  const errors:string[]=[]; const party=new Set(partyIds); const required:JourneyRole[]=['guide','scout','hunter','lookout'];
  for(const role of required){
    const id=roles[role];
    if(!id) errors.push('A Companhia precisa definir '+JOURNEY_ROLE_LABELS[role]+' ('+JOURNEY_ROLE_SKILLS[role]+').');
    else if(!party.has(id)) errors.push(JOURNEY_ROLE_LABELS[role]+': personagem fora da Companhia.');
  }
  if(partyIds.length>=4){
    const dualLookout=new Set(naturalWatchfulnessIds);
    const assignments=new Map<string,JourneyRole[]>();
    for(const role of required){
      const id=roles[role];
      if(!id) continue;
      assignments.set(id,[...(assignments.get(id)||[]),role]);
    }
    for(const [id,held] of assignments){
      if(held.length<=1) continue;
      const onlyAllowed=held.length===2 && held.includes('lookout') && dualLookout.has(id);
      if(!onlyAllowed) errors.push('Com quatro ou mais Heróis, cada função coberta deve ficar com um Herói diferente; apenas Vigilância Natural permite acumular Vigia com outra função.');
    }
  }
  return errors;
}"""
t=req(t,old,new,"Journey role validator")
write(rel,t)

# ---------------------------------------------------------------------------
# Commands: selected Beorn skills; role-validator exception; fellowship bonus
# from Honey Cakes when newly acquired.
# ---------------------------------------------------------------------------
rel="lib/master/commands.ts"
t=read(rel)
# characterRules import can vary.
t=t.replace(
    "import { findSkill, targetNumberForSkill } from '../characterRules';",
    "import { attributeForSkill, findSkill, targetNumberForSkill } from '../characterRules';",
    1,
)
if "supplementCultureFor" not in t:
    t=t.replace(
        "import {",
        "import {",
        1,
    )

# Insert Beorn command before player journey start.
anchor="    case 'player_start_journey': {"
beorn="""    case 'set_beorn_enchanted_skills': {
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero || !hasVirtue(hero.sheet,'O Encantamento de Beorn')) throw new Error('O Encantamento de Beorn não está disponível para este Herói.');
      const selected=Array.from(new Set((command.skills||[]).map(value=>String(value).trim()).filter(Boolean)));
      if(selected.length!==3) throw new Error('O Encantamento de Beorn exige exatamente três Perícias de Força.');
      for(const skillName of selected){
        if(!findSkill(hero.sheet,skillName)) throw new Error('Perícia não encontrada: '+skillName);
        if(attributeForSkill(skillName)!=='strength') throw new Error(skillName+' não é uma Perícia de Força.');
      }
      updateHero(hero.id,sheet=>{
        const next=structuredClone(sheet);
        next.other.supplementSelections={...(next.other.supplementSelections||{}),beornEnchantedSkills:selected};
        return next;
      });
      break;
    }

"""
t=req(t,anchor,beorn+anchor,"Beorn selection command")

# The player-start Journey validator receives Natural Watchfulness holders.
# Accept both the older striderNoRoles layout and the current explicit Strider branch.
natural_line="      const naturalWatchfulnessIds=nextCharacters.filter(character=>partyIds.has(character.id)&&hasVirtue(character.sheet,'Vigilância Natural')).map(character=>character.id);"
legacy_line="      const roleErrors = striderNoRoles ? [] : validateJourneyRoles(roles, Array.from(partyIds));"
branch_line="        const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));"
if natural_line not in t:
    if legacy_line in t:
        t=t.replace(
            legacy_line,
            natural_line+"\n      const roleErrors = striderNoRoles ? [] : validateJourneyRoles(roles, Array.from(partyIds), naturalWatchfulnessIds);",
            1,
        )
    elif branch_line in t:
        t=t.replace(
            branch_line,
            "        const naturalWatchfulnessIds=nextCharacters.filter(character=>partyIds.has(character.id)&&hasVirtue(character.sheet,'Vigilância Natural')).map(character=>character.id);\n        const roleErrors = validateJourneyRoles(roles, Array.from(partyIds), naturalWatchfulnessIds);",
            1,
        )
    else:
        raise SystemExit("missing anchor: Natural Watchfulness role exception")

# Apply Bolos de Mel fellowship bonus when acquired through canonical Wisdom advancement.
old="""      else updateHero(command.targetCharacterId, sheet => { const result = advanceWisdom(sheet, command.virtue); if (!result.allowed) throw new Error(result.reason); return result.sheet; });
      nextState.fellowshipPhase.valorWisdomAdvance"""
new="""      else {
        const beforeHero=nextCharacters.find(character=>character.id===command.targetCharacterId);
        const hadHoney=Boolean(beforeHero&&hasVirtue(beforeHero.sheet,'Bolos de Mel Duas Vezes Assados'));
        updateHero(command.targetCharacterId, sheet => { const result = advanceWisdom(sheet, command.virtue); if (!result.allowed) throw new Error(result.reason); return result.sheet; });
        const afterHero=nextCharacters.find(character=>character.id===command.targetCharacterId);
        if(!hadHoney && afterHero && hasVirtue(afterHero.sheet,'Bolos de Mel Duas Vezes Assados')){
          nextState.company={...nextState.company,fellowshipMax:nextState.company.fellowshipMax+1,fellowshipCurrent:nextState.company.fellowshipCurrent+1};
        }
      }
      nextState.fellowshipPhase.valorWisdomAdvance"""
t=req(t,old,new,"Honey Cakes fellowship acquisition")
write(rel,t)

# ---------------------------------------------------------------------------
# Supplement UI: configure exactly three Strength skills for Beorn Enchantment.
# ---------------------------------------------------------------------------
rel="components/SupplementRulesPanel.tsx"
t=read(rel)
t=req(
    t,
    "import { hasVirtue, hunterResolveRecovery } from '../lib/rules/supplementMechanics';",
    "import { hasVirtue, hunterResolveRecovery } from '../lib/rules/supplementMechanics';\nimport { attributeForSkill } from '../lib/characterRules';",
    "Supplement UI attribute import",
)
t=req(
    t,
    "  const [message,setMessage]=useState('');\n",
    """  const [message,setMessage]=useState('');
  const [beornSkills,setBeornSkills]=useState<string[]>([]);
""",
    "Beorn UI state",
)
# Reset selection when hero changes / sheet updates.
insert_anchor="  const hero=characters.find(item=>item.id===heroId)||characters[0];\n"
insert="""  const hero=characters.find(item=>item.id===heroId)||characters[0];
  React.useEffect(()=>{
    setBeornSkills([...(hero?.sheet.other.supplementSelections?.beornEnchantedSkills||[])]);
  },[hero?.id,hero?.sheet.other.supplementSelections?.beornEnchantedSkills?.join('|')]);
"""
t=req(t,insert_anchor,insert,"Beorn UI sync")

hero_end="""        {hasVirtue(hero.sheet,'Poder do Primogênito')&&masterState.activeMode==='combat'&&<button"""
beorn_ui="""        {hasVirtue(hero.sheet,'O Encantamento de Beorn')&&<div className="mt-3 rounded border border-amber-900/60 p-2">
          <div className="text-xs font-black text-amber-100">O Encantamento de Beorn · escolha 3 Perícias de Força</div>
          <div className="mt-2 grid grid-cols-2 gap-1">{hero.sheet.skills.filter(skill=>attributeForSkill(skill.name)==='strength').map(skill=><label key={skill.name} className="flex items-center gap-2 text-[11px]"><input type="checkbox" checked={beornSkills.includes(skill.name)} onChange={e=>setBeornSkills(current=>e.target.checked?Array.from(new Set([...current,skill.name])).slice(0,3):current.filter(name=>name!==skill.name))}/>{skill.name}</label>)}</div>
          <button disabled={busy||beornSkills.length!==3} onClick={()=>void run([{type:'set_beorn_enchanted_skills',targetCharacterId:hero.id,skills:beornSkills,reason:'Três Perícias de Força escolhidas para O Encantamento de Beorn.'} as MasterCommand],'Configurar O Encantamento de Beorn')} className="mt-2 w-full rounded border border-amber-700 px-3 py-2 text-xs disabled:opacity-40">Salvar 3 Perícias</button>
        </div>}
"""
t=req(t,hero_end,beorn_ui+hero_end,"Beorn UI controls")
write(rel,t)

# ---------------------------------------------------------------------------
# MechanicsHub: expose Elrond's Meet Patron undertaking when valid.
# ---------------------------------------------------------------------------
rel="components/MechanicsHub.tsx"
t=read(rel)
anchor="""        <button disabled={busy} onClick={()=>void run([{type:'record_undertaking',targetCharacterId:selectedProgress?.id||'',undertaking:'strengthen-fellowship',reason:'Empreendimento Fortalecer Sociedade.'} as MasterCommand],'Fortalecer Sociedade')} className="col-span-2 rounded border border-cyan-800 px-3 py-2">Empreendimento: Fortalecer Sociedade</button>"""
addition=anchor+"""
        {/elrond/i.test(String(masterState.company.patron||''))&&<button disabled={busy||!selectedProgress||masterState.fellowshipPhase.status!=='active'} onClick={()=>selectedProgress&&void run([{type:'record_undertaking',targetCharacterId:selectedProgress.id,undertaking:'meet-patron',reason:'Empreendimento Encontrar Patrono — Elrond.'} as MasterCommand],'Encontrar Patrono · Elrond')} className="col-span-2 rounded border border-violet-800 px-3 py-2 disabled:opacity-40">Empreendimento: Encontrar Patrono · Elrond</button>}"""
t=req(t,anchor,addition,"Elrond undertaking UI")
write(rel,t)

print("PATCH_4939_VIRTUES_OK")
