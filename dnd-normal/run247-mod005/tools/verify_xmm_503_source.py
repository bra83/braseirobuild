#!/usr/bin/env python3
"""XMM-2025: compare every compiled and indexed monster against independent pinned
source JSON, plus the 36 source-bound book sections. Never certify just a class count.
No original statblock prose is republished in the Android app.
"""
import argparse, hashlib, json, re, time, urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
PIN="8c026b807fac21862a309379a0c3228a0683198b"
BESTIARY_SHA="9a4137b508ef37562ed948894a70b1594b57e85c"
BOOK_SHA="a21801793df3515d07ef9a56d9febefea1c6f8f7"
FILES=("data/bestiary/bestiary-xmm.json","data/book/book-xmm.json")
SOURCE="https://raw.githubusercontent.com/5etools-mirror-3/5etools-src/"+PIN+"/"
JAVA_INDEX="app/src/main/java/com/braseiro/dnd5e/Xmm2025SourceIndex.java"
JAVA_BOOK="app/src/main/java/com/braseiro/dnd5e/Xmm2025BookIndex.java"
JAVA_SPELLS="app/src/main/java/com/braseiro/dnd5e/Xmm2025SpellUsageIndex.java"
JAVA_TRAITS="app/src/main/java/com/braseiro/dnd5e/Xmm2025TraitMechanicsEngine.java"
JAVA_RUNTIME="app/src/main/java/com/braseiro/dnd5e/Xmm2025CompiledBestiaryEngine.java"
JAVA_ATTACK_CONDITIONS="app/src/main/java/com/braseiro/dnd5e/Xmm2025AttackConditionEngine.java"
JAVA_SAVE_DAMAGE="app/src/main/java/com/braseiro/dnd5e/Xmm2025SaveDamageEngine.java"
LITERAL=re.compile(r'^\s*("(?:\\.|[^"\\])*")\s*,?\s*$',re.M)
RCH=re.compile(r'\s*\{@recharge(?: (\d))?\}')
SP=re.compile(r'\{@spell ([^|}]+)')

def blob_sha(b):
    return hashlib.sha1(b"blob "+str(len(b)).encode()+b"\0"+b).hexdigest()

def java_rows(text):
    result=[]
    for match in LITERAL.finditer(text):
        item=json.loads(json.loads(match.group(1)))
        if not isinstance(item,dict) or "id" not in item and "key" not in item:
            raise ValueError("XMM_JAVA_INDEX_HAS_NON_ROW")
        result.append(item)
    return result

def monster_type(data):
    if isinstance(data,str): return data
    t=data.get("type",data)
    if isinstance(t,str): return t
    if isinstance(t,dict) and "choose" in t: return "|".join(t["choose"])
    raise ValueError("XMM_UNKNOWN_TYPE:"+str(data))

def cr_float(data):
    if isinstance(data,dict):data=data.get("cr",data.get("lair",data.get("coven")))
    if "/" in str(data):
        a,b=str(data).split("/",1)
        return float(a)/float(b)
    return float(data)

def recharge_rows(source):
    out=[]
    for field,kind in (("trait","trait"),("action","action"),("bonus","bonus_action"),
                       ("reaction","reaction"),("legendary","legendary_action")):
        for i,r in enumerate(source.get(field,[])):
            name=r["name"]
            matched=RCH.search(name)
            if matched:
                out.append({"cat":kind,"index":i,"name":RCH.sub("",name).strip(),
                            "min":int(matched.group(1) or "6")})
    return out

def check_grapple_escape_riders(source, compiled, contracts, expected_count=36):
    """Check source-pinned grapple escape DCs are never compiled as saves."""
    problems=[]
    checked=0
    for rider in contracts:
        if rider.get("condition")!="grappled":
            continue
        monster=rider["id"]
        source_row=next((x for x in source if x.get("name")==monster),None)
        compiled_row=next((x for x in compiled if x.get("id")==monster),None)
        if source_row is None or compiled_row is None:
            problems.append(monster+":GRAPPLE_CREATURE_MISSING")
            continue
        slot=rider["slot"]
        try:
            src=source_row["action"][slot]
            dst=compiled_row["actions"][slot]
        except (KeyError,IndexError,TypeError):
            problems.append(monster+":"+rider["name"]+":GRAPPLE_ACTION_MISSING")
            continue
        if src.get("name")!=rider.get("name") or dst.get("name")!=rider.get("name"):
            problems.append(monster+":"+rider["name"]+":GRAPPLE_ACTION_SLOT_MISMATCH")
            continue
        text=json.dumps(src.get("entries",[]),ensure_ascii=False)
        dc=int(rider["dc"])
        source_reach=re.search(r"\breach\s+(\d+)\s*ft\.?",text,re.I)
        if "{@condition Grappled" not in text or ("escape {@dc %d}"%dc) not in text or \
           "saving throw" in text.lower() or "escape" not in text.lower() or not source_reach:
            problems.append(monster+":"+rider["name"]+":PINNED_ESCAPE_SOURCE_CHANGED")
            continue
        source_saves=re.findall(r"\{@actSave\s+(str|dex|con|int|wis|cha)\}\s*\{@dc\s*(\d+)",text,re.I)
        save_mismatch=(bool(source_saves) and (len(source_saves)!=1 or
            dst.get("save_ability","").lower()!=source_saves[0][0].lower() or
            dst.get("save_dc")!=int(source_saves[0][1]) or not dst.get("condition_on_fail")))
        save_unproven=(not source_saves and any(k in dst for k in ("save_dc","save_ability","condition_on_fail")))
        if dst.get("reach_ft")!=int(source_reach.group(1)):
            problems.append(monster+":"+rider["name"]+":GRAPPLE_REACH_MISMATCH")
            continue
        semantic_ok=(dst.get("escape_dc")==dc and
            dst.get("condition_max_target_size")==rider.get("size","").lower() and
            "grappled" in dst.get("condition_on_hit","").lower() and
            not save_mismatch and not save_unproven)
        manual_ok=(bool(dst.get("manual_rider_resolution_required",False))==
            bool(rider.get("other",False)))
        if not semantic_ok:
            problems.append(monster+":"+rider["name"]+":ESCAPE_DC_MISCLASSIFIED")
        if not manual_ok:
            problems.append(monster+":"+rider["name"]+":MANUAL_RIDER_FLAG_MISMATCH")
        if semantic_ok and manual_ok:
            checked+=1
    if checked!=expected_count:
        problems.append("GRAPPLE_ESCAPE_RECORD_COUNT:"+str(checked))
    return problems,checked

SAVE_DAMAGE=re.compile(r"\{@actSave\s+(str|dex|con|int|wis|cha)\}\s*\{@dc\s*(\d+)\}"
    r".*?(\d+)\s*\(\{@damage\s+([^}]+)\}\)\s+([A-Za-z]+) damage\.",re.I|re.S)
def check_save_damage_actions(source, contracts, expected_count=59):
    """Bind every simple save-for-half contract to its pinned source action."""
    problems=[]; checked=0; seen=set()
    for row in contracts:
        monster=row.get("id",""); name=row.get("name",""); category=row.get("category","")
        pointer=row.get("source_pointer","")
        if pointer in seen:
            problems.append(monster+":"+name+":DUPLICATE_SAVE_SOURCE_POINTER"); continue
        seen.add(pointer)
        match=re.fullmatch(r"/monster/(\d+)/(action|bonus)/(\d+)/entries/0",pointer)
        if not match:
            problems.append(monster+":"+name+":INVALID_SAVE_SOURCE_POINTER"); continue
        mi,source_category,slot=match.group(1),match.group(2),int(match.group(3))
        if source_category != category:
            problems.append(monster+":"+name+":SAVE_CATEGORY_MISMATCH"); continue
        try:
            src=source[int(mi)]
            actions=src.get(source_category,[])
            action=actions[slot]
        except (IndexError,TypeError):
            problems.append(monster+":"+name+":SAVE_ACTION_MISSING"); continue
        if src.get("name")!=monster or RCH.sub("",action.get("name","")).strip()!=name:
            problems.append(monster+":"+name+":SAVE_ACTION_SLOT_MISMATCH"); continue
        entries=action.get("entries",[])
        text=entries[0] if entries and isinstance(entries[0],str) else ""
        parsed=SAVE_DAMAGE.search(text)
        if not parsed:
            problems.append(monster+":"+name+":SOURCE_NOT_SIMPLE_SAVE_FOR_HALF"); continue
        ability,dc,average,dice,damage_type=(parsed.group(1).lower(),int(parsed.group(2)),
            int(parsed.group(3)),parsed.group(4).strip(),parsed.group(5).lower())
        success=re.search(r"\{@actSaveSuccess\}\s*Half damage\.",text,re.I)
        if not success:
            problems.append(monster+":"+name+":SOURCE_SUCCESS_DAMAGE_RULE_MISMATCH"); continue
        source_recharge="{@recharge" in action.get("name","")
        if (row.get("ability")!=ability or row.get("dc")!=dc or row.get("average")!=average or
            row.get("dice")!=dice or row.get("damage_type","").lower()!=damage_type or
            row.get("recharge")!=source_recharge):
            problems.append(monster+":"+name+":SAVE_DAMAGE_SEMANTICS_MISMATCH"); continue
        checked+=1
    if checked!=expected_count: problems.append("SAVE_DAMAGE_RECORD_COUNT:"+str(checked))
    return problems,checked


def check_monsters(source,index,compiled,attack_contracts=None):
    problems=[]
    if len(source)!=503 or len(index)!=503 or len(compiled)!=503:
        raise ValueError(f"XMM_NOT_503:{len(source)}:{len(index)}:{len(compiled)}")
    if len({x["name"] for x in source})!=503 or len({x["id"] for x in index})!=503:
        raise ValueError("XMM_DUPLICATE_MONSTERS")
    recharges,spellcasters=0,0
    grapple_errors,grapple_count=check_grapple_escape_riders(source,compiled,attack_contracts or [])
    problems.extend(grapple_errors)
    for i,(s,e,c) in enumerate(zip(source,index,compiled)):
        monster=s["name"]
        if not(e["id"]==c["id"]==monster and e["ptr"]=="/monster/"+str(i)):
            problems.append(f"{i}:NAME_OR_POINTER")
            continue
        ac=s["ac"][0]
        if isinstance(ac,dict):ac=ac["ac"]
        if ac!=e["ac"] or ac!=c["armor_class"]:problems.append(monster+":AC")
        if s["hp"]["average"]!=e["hp"] or e["hp"]!=c["hit_points"]:problems.append(monster+":HP")
        if s["hp"].get("formula")!=e.get("formula") or e.get("formula")!=c.get("hit_points_formula"):
            problems.append(monster+":HP_FORMULA")
        if monster_type(s["type"])!=e["type"] or e["type"]!=c["creature_type"]:
            problems.append(monster+":TYPE")
        if cr_float(s["cr"])!=cr_float(e["cr"]) or cr_float(e["cr"])!=c["challenge_rating"]:
            problems.append(monster+":CR")
        for key in ("str","dex","con","int","wis","cha"):
            if s[key]!=e["abilities"][key] or s[key]!=c["abilities"][key]:
                problems.append(monster+":ABILITY:"+key)
        if (s.get("environment") or [])!=e["habitats"]:problems.append(monster+":HABITATS")
        if (s.get("group") or "")!=e["group"]:problems.append(monster+":GROUP")
        for field,kind in (("trait","traits"),("action","actions")):
            if len(s.get(field,[]))!=len(c[kind]):problems.append(monster+":COUNT:"+field)
        counts={"traits":len(s.get("trait",[])),"actions":len(s.get("action",[])),
                "bonus":len(s.get("bonus",[])),"reaction":len(s.get("reaction",[])),
                "legendary":len(s.get("legendary",[])),"spells":len(s.get("spellcasting",[]))}
        if counts!=e["counts"]:problems.append(monster+":COUNTS")
        expected=recharge_rows(s)
        if e["recharges"]!=expected:problems.append(monster+":RECHARGE_METADATA")
        recharges+=len(expected)
        if s.get("spellcasting"):spellcasters+=1
        names=list(dict.fromkeys(SP.findall(json.dumps(s.get("spellcasting",[])))))
        if names!=e["spells"]:problems.append(monster+":SPELL_LIST")
        for field,out,category in (("trait","traits",None),("action","actions",None),
                                   ("bonus","special_actions","bonus_action"),
                                   ("reaction","special_actions","reaction"),
                                   ("legendary","special_actions","legendary_action")):
            dest=c[out]
            if category:dest=[a for a in dest if a.get("category")==category]
            src=s.get(field,[])
            if [RCH.sub("",q["name"]).strip() for q in src]!=[q["name"] for q in dest]:
                problems.append(monster+":ABILITY_NAMES:"+field)
    if recharges!=120 or spellcasters!=132:
        problems.append(f"SOURCE_TOTALS:{recharges}:{spellcasters}")
    return problems,{"monsters":len(source),"recharge_actions":recharges,
                     "spellcasting_monsters":spellcasters,
                     "grapple_escape_records":grapple_count}

def check_conditional_attack_damage(source,compiled,expected_bloodied=13,expected_manual=5):
    """Ensure alternate attack damage is selected or the action stays manual.

    A second damage expression is not automatically additive. This pins the
    Bloodied branches used by the runtime and requires other alternate/extra
    damage clauses to remain explicitly manual until their condition is modeled.
    """
    by_id={m["id"]:m for m in compiled}
    errors=[]; bloodied_count=0; manual_count=0
    bloodied_status=re.compile(r"\{@status\s+Bloodied(?:\|[^}]*)?\}",re.I)
    damage=re.compile(r"\{@damage\s+([^}]+)\}",re.I)
    alternate=re.compile(r"(?:,\s*or|—or)\s*\d+\s*\(\{@damage\s+([^}]+)\}\)",re.I)
    suffix=re.compile(r"^[\s—-]*plus\s+\d+\s*\(\{@damage\s+([^}]+)\}\)\s+([A-Za-z]+)\s+damage\b",re.I)
    for monster in source:
        dst=by_id.get(monster["name"],{})
        for slot,action in enumerate(monster.get("action",[])):
            entries=action.get("entries",[])
            entry=" ".join(x for x in entries if isinstance(x,str))
            compiled_actions=dst.get("actions",[])
            compiled_action=compiled_actions[slot] if slot<len(compiled_actions) else {}
            status=bloodied_status.search(entry)
            alt=alternate.search(entry)
            if status and alt:
                bloodied_count+=1
                expressions=damage.findall(entry)
                if len(expressions)<2 or expressions[1].strip()!=alt.group(1).strip():
                    errors.append(f"{monster['name']}:{action['name']}:BLOODIED_SOURCE_BRANCH_SHAPE")
                    continue
                tail=entry[status.end():]
                extra=suffix.search(tail)
                extra_expr=extra.group(1).strip() if extra else ""
                expected_base=expressions[0].strip()+(" + "+extra_expr if extra_expr else "")
                expected_alt=expressions[1].strip()+(" + "+extra_expr if extra_expr else "")
                base_match=re.search(r"\{@damage\s+"+re.escape(expressions[0])+r"\}\)\s*([A-Za-z]+)\s+damage",entry,re.I)
                extra_type=extra.group(2).lower() if extra else ""
                expected_type=(base_match.group(1).lower() if base_match else "")
                if extra_type: expected_type += " + "+extra_type
                if compiled_action.get("damage")!=expected_base:
                    errors.append(f"{monster['name']}:{action['name']}:BASE_DAMAGE:{compiled_action.get('damage')}!={expected_base}")
                if compiled_action.get("damage_bloodied")!=expected_alt:
                    errors.append(f"{monster['name']}:{action['name']}:BLOODIED_DAMAGE:{compiled_action.get('damage_bloodied')}!={expected_alt}")
                if compiled_action.get("damage_type","").lower()!=expected_type:
                    errors.append(f"{monster['name']}:{action['name']}:BLOODIED_DAMAGE_TYPE:{compiled_action.get('damage_type')}!={expected_type}")
                source_requires_manual=("{@condition Grappled" in entry or "{@variantrule Advantage" in entry)
                if bool(compiled_action.get("manual_rider_resolution_required",False))!=source_requires_manual:
                    errors.append(f"{monster['name']}:{action['name']}:BLOODIED_OTHER_RIDER_MANUAL_FLAG")
            elif alt:
                manual_count+=1
                if not compiled_action.get("manual_rider_resolution_required",False):
                    errors.append(f"{monster['name']}:{action['name']}:UNMODELED_ALTERNATE_DAMAGE_NOT_MANUAL")
            if re.search(r"\bif\b.{0,220}\b(?:takes|deals) an extra\s+\d+\s*\(\{@damage",entry,re.I) and \
               not compiled_action.get("manual_rider_resolution_required",False):
                errors.append(f"{monster['name']}:{action['name']}:CONDITIONAL_EXTRA_DAMAGE_NOT_MANUAL")
    if bloodied_count!=expected_bloodied: errors.append(f"BLOODIED_BRANCH_TOTAL:{bloodied_count}:{expected_bloodied}")
    if manual_count!=expected_manual: errors.append(f"UNMODELED_ALTERNATE_DAMAGE_TOTAL:{manual_count}:{expected_manual}")
    return errors,{"source_bloodied_attack_branches":bloodied_count,
                   "source_alternate_attack_branches_manual":manual_count}

UNMODELED_ACTION_EFFECTS=re.compile(
    r"(?:\b(?:advantage|disadvantage|cursed?|curse|wound|pushed|pulled|"
    r"reactions?|speed.{0,24}(?:halved|reduced|decreases?)|"
    r"(?:strength|dexterity|constitution|intelligence|wisdom|charisma) score decreases|"
    r"hit points?.{0,100}maximum decreases|temporary hit points|another creature|other creature|choice creature|"
    r"creature within \d+ feet of the target|"
    r"can.t regain|doesn.t regain|no benefit from finishing|"
    r"creates one of the following effects|one of the .*? choice|"
    r"\{@hom\}|\{@hazard burning|starts? (?:to )?burning|"
    r"(?:pushes|pulls|moves) the target|teleport the target|"
    r"attaches to the target|while attached|removing its heart|"
    r"kills the target by)\b|\{@hom\}|\{@hazard burning\b)",re.I)

def source_action_requires_manual_resolution(action):
    """Catch source attack effects that are not part of the typed runtime contract."""
    def flatten(value):
        if isinstance(value,str): return value
        if isinstance(value,list): return " ".join(flatten(x) for x in value)
        if isinstance(value,dict): return " ".join(flatten(x) for x in value.values())
        return ""
    text=flatten(action.get("entries",[]))
    # Compound conditions and recurring saves need a typed condition lifecycle;
    # returning a comma-separated condition string does not implement that lifecycle.
    conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
    recurring=bool(re.search(r"\brepeats? the save\b|\bat the end of each of its turns\b",text,re.I))
    return bool(UNMODELED_ACTION_EFFECTS.search(text)) or len(conditions)>1 or recurring

def source_action_has_typed_compound_condition_contract(monster_name,action,compiled_action):
    """Narrowly typed source condition bundles with an exact source-turn expiry."""
    def flatten(value):
        if isinstance(value,str): return value
        if isinstance(value,list): return " ".join(flatten(x) for x in value)
        if isinstance(value,dict): return " ".join(flatten(x) for x in value.values())
        return ""
    text=flatten(action.get("entries",[]))
    name=RCH.sub("",action.get("name","")).strip()
    if monster_name=="Rakshasa" and name=="Baleful Command":
        expected={"frightened","incapacitated"}; field="condition_on_fail"
        source_until="until the start of the rakshasa's next turn"
    elif monster_name=="Storm Giant" and name=="Thunderbolt":
        expected={"blinded","deafened"}; field="condition_on_hit"
        source_until="until the start of the giant's next turn"
    else:return False
    source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
    compiled_conditions={x.strip().casefold() for x in compiled_action.get(field,"").split(",")}
    return (source_conditions==expected and compiled_conditions==expected and
        source_until in text.casefold() and
        not re.search(r"\brepeats? the save\b|\bat the end of each of its turns\b|\bchoice\b",text,re.I) and
        compiled_action.get("condition_duration_rounds")==1 and
        compiled_action.get("condition_duration_tick")=="source_turn_start" and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def check_unmodeled_action_effects(source,compiled):
    """Require a dedicated fail-closed flag for known untyped source effects."""
    by_id={m["id"]:m for m in compiled}; errors=[]; checked=0; typed_compound=0
    for monster in source:
        dst=by_id.get(monster.get("name"),{})
        categories=(("action","actions",None),("bonus","special_actions","bonus_action"),
                    ("reaction","special_actions","reaction"),("legendary","special_actions","legendary_action"))
        for source_field,dest_field,category in categories:
            destination=dst.get(dest_field,[])
            if category: destination=[a for a in destination if a.get("category")==category]
            for slot,action in enumerate(monster.get(source_field,[])):
                if not source_action_requires_manual_resolution(action): continue
                checked+=1
                compiled_action=destination[slot] if slot<len(destination) else {}
                expected_name=RCH.sub("",action.get("name","")).strip()
                if compiled_action.get("name")!=expected_name:
                    errors.append(f"{monster['name']}:{action.get('name','?')}:UNTYPED_SOURCE_ACTION_SLOT_MISMATCH")
                if source_action_has_typed_compound_condition_contract(monster['name'],action,compiled_action):
                    typed_compound+=1
                    continue
                if not compiled_action.get("manual_unmodeled_source_effect_required",False):
                    errors.append(f"{monster['name']}:{action.get('name','?')}:UNTYPED_SOURCE_EFFECT_NOT_MANUAL")
    return errors,checked-typed_compound

def usage_rows(source):
    out=[]
    for monster in source:
        if not monster.get("spellcasting"):
            continue
        entries=[]
        for spellcasting in monster["spellcasting"]:
            row={"name":spellcasting["name"],
                 "display_as":spellcasting.get("displayAs","action"),
                 "spellcasting_ability":spellcasting.get("ability",""),
                 "will":SP.findall(json.dumps(spellcasting.get("will",[])))}
            for key in ("daily","restLong","legendary","recharge"):
                row[key]={k:SP.findall(json.dumps(v)) for k,v in spellcasting.get(key,{}).items()}
            entries.append(row)
        ptr=f"/monster/{source.index(monster)}/spellcasting"
        out.append({"id":monster["name"],"source_pointer":ptr,"entries":entries})
    return out

def deref(book,p):
    obj=book
    for part in p.strip("/").split("/"):
        obj=obj[int(part)] if isinstance(obj,list) else obj[part]
    return obj

def creature_refs(x):
    if isinstance(x,list):
        return [name for it in x for name in creature_refs(it)]
    if not isinstance(x,dict):return []
    return ([x["name"]] if x.get("type")=="statblock" and x.get("tag")=="creature" else [])+creature_refs(x.get("entries",[]))

def source_creature_names_from_markup(value):
    """Only creature references, never broad substring matches or inferred names."""
    return re.findall(r'\{@creature ([^|}]+)',json.dumps(value,ensure_ascii=False))

def source_habitat_groups(ref):
    return {node["caption"]:source_creature_names_from_markup(node.get("rows",[]))
            for node in ref.get("entries",[]) if isinstance(node,dict) and node.get("type")=="table"}

def source_named_groups(ref):
    lists=[node for node in ref.get("entries",[]) if isinstance(node,dict) and node.get("type")=="list"]
    if len(lists)!=1:
        raise ValueError("XMM_GROUP_LIST_STRUCTURE_CHANGED")
    return {group["name"]:source_creature_names_from_markup(group.get("items",[]))
            for group in lists[0].get("items",[]) if isinstance(group,dict) and group.get("name")}

def check_book(book,rows):
    problems=[]
    if len(rows)!=36 or len({r["key"] for r in rows})!=36:
        return ["XMM_36_SECTION_COUNT"]
    required={("ch_0:sec_0_"+str(i)) for i in range(4)}
    required.update("ch_1:sec_1_"+str(i) for i in range(25))
    required.update({"ch_2:sec_2_0","ch_3:sec_3_0","ch_3:sec_3_1",
                     "ch_3:sec_3_2","ch_3:sec_3_3","ch_3:sec_3_4","ch_4:sec_4_0"})
    if {r["key"] for r in rows}!=required:problems.append("XMM_SECTION_KEYS_DIFFER")
    for r in rows:
        k=r["key"]
        try:ref=deref(book,r["pointer"])
        except (IndexError,KeyError,TypeError,ValueError):problems.append(k+":INVALID_POINTER");continue
        if not isinstance(ref,dict):problems.append(k+":NONOBJECT_SECTION");continue
        if r["title"]!=ref.get("name"):problems.append(k+":TITLE_DIFF")
        if r["page"]!=ref.get("page"):problems.append(k+":PAGE_DIFF")
        if k.startswith("ch_1:"):
            if r["names"]!=creature_refs(ref):problems.append(k+":ALPHABET_CREATURE_NAMES")
        elif k=="ch_2:sec_2_0":
            expected=source_creature_names_from_markup(ref["entries"][2]["items"])
            if r["names"]!=expected:problems.append(k+":ANIMAL_NAMES")
        elif k=="ch_3:sec_3_0":
            expected=[[source_creature_names_from_markup(cell) for cell in row]
                      for row in ref["entries"][1]["rows"]]
            if r["conversion_rows"]!=expected or len(expected)!=59:
                problems.append(k+":EXACT_2014_2025_CONVERSIONS")
        elif k=="ch_3:sec_3_1":
            expected=source_habitat_groups(ref)
            if r["groups"]!=expected or len(expected)!=12:
                problems.append(k+":EXACT_HABITAT_GROUPS")
        elif k in ("ch_3:sec_3_2","ch_3:sec_3_3","ch_3:sec_3_4"):
            expected=source_named_groups(ref)
            sizes={"ch_3:sec_3_2":14,"ch_3:sec_3_3":12,"ch_3:sec_3_4":30}
            if r["groups"]!=expected or len(expected)!=sizes[k]:
                problems.append(k+":EXACT_MONSTER_GROUPS")
        elif k=="ch_0:sec_0_3":
            expected=[entry["name"] for entry in ref["entries"]
                      if isinstance(entry,dict) and entry.get("name")]
            if r.get("parts")!=expected or len(expected)!=23:
                problems.append(k+":EXACT_23_STATBLOCK_PARTS")
        elif k=="ch_0:sec_0_2":
            expected=[x.get("name","").lower().replace(" ","_")
                      for x in ref["entries"] if isinstance(x,dict) and x.get("name")]
            if r.get("fields")!=expected:
                problems.append(k+":EXACT_5_MONSTER_ENTRY_COMPONENTS")
        elif k=="ch_0:sec_0_1":
            explanatory=json.dumps(ref["entries"],ensure_ascii=False)
            required_terms=("Armor Class","Hit Points","Speed","Initiative","Ability Scores",
                            "Traits","Actions","Reactions","Legendary Actions")
            if any(word not in explanatory for word in required_terms):
                problems.append(k+":STATBLOCK_GLOSSARY_SOURCE_CHANGED")
            if len(r.get("fields",[]))<9:
                problems.append(k+":STATBLOCK_RUNTIME_FIELDS_INCOMPLETE")
        elif k=="ch_0:sec_0_0":
            source_text=json.dumps(ref["entries"],ensure_ascii=False).lower()
            requirements=("more than 500 stat blocks","alphabetically","2014","replacement")
            if any(term not in source_text for term in requirements):
                problems.append(k+":2025_CHANGE_SEMANTICS")
            if len(r.get("rules",[]))<4:
                problems.append(k+":2025_CHANGE_RULE_COVERAGE")
        elif k=="ch_4:sec_4_0":
            if not r.get("editorial_only"):problems.append(k+":CREDITS_NOT_EDITORIAL")
    return problems

TRAIT_KINDS={"Magic Resistance":["Advantage|XPHB} on saving throws against spells and other magical effects."],"Legendary Resistance":["fails a saving throw, it can choose to succeed instead."],"Pack Tactics":["Advantage|XPHB} on","attack roll","within 5 feet of the creature","doesn't have the {@condition Incapacitated"],"Sunlight Sensitivity":["While in sunlight","Disadvantage|XPHB} on ability checks and attack rolls."],"Flyby":["doesn't provoke","Opportunity Attack","when it flies out of an enemy's reach."],"Siege Monster":["deals double damage to objects and structures."],"Evasion":["Dexterity saving throw to take only half damage","no damage if it succeeds","half damage if it fails","Incapacitated"],"Undead Fortitude":["damage reduces the zombie to 0","{@dc 5} plus the damage taken","Radiant","Critical Hit","drops to 1"]}
def trait_rows(source):
    result=[]
    for i,monster in enumerate(source):
        for j,t in enumerate(monster.get("trait",[])):
            kind=re.sub(r"\s*\(.*\)$","",t["name"])
            if kind not in TRAIT_KINDS:continue
            entries=t.get("entries")
            if not isinstance(entries,list) or len(entries)!=1 or not isinstance(entries[0],str):
                raise ValueError("XMM_TRAIT_ENTRY_SHAPE_CHANGED:"+monster["name"])
            body=entries[0]
            if any(term not in body for term in TRAIT_KINDS[kind]):
                raise ValueError("XMM_TRAIT_SOURCE_SEMANTICS_CHANGED:"+monster["name"]+":"+kind)
            uses=re.search(r"\((\d+)/Day(?:, or (\d+)/Day in Lair)?\)",t["name"])
            if kind=="Legendary Resistance" and not uses:
                raise ValueError("XMM_LEGENDARY_USES_NOT_PINNED:"+monster["name"])
            result.append({
                "id":monster["name"],"kind":kind,"trait":t["name"],"slot":j,
                "ptr":f"/monster/{i}/trait/{j}/entries/0",
                "uses":int(uses.group(1)) if uses else 0,
                "lair_uses":int(uses.group(2) or uses.group(1)) if uses else 0,
            })
    if len(result)!=178 or len({r["kind"] for r in result})!=8:
        raise ValueError("XMM_TRAIT_178_8_SOURCE_TOTAL_CHANGED:"+str(len(result)))
    return result

def fetch_source_bytes(url, user_agent="Braseiro-XMM-Provenance/1.0", max_bytes=5*1024*1024,
                      attempts=3, opener=None):
    """Retry transient source mirror timeouts without changing the pinned URL."""
    open_url=opener or urllib.request.urlopen
    request=urllib.request.Request(url,headers={"User-Agent":user_agent})
    last_error=None
    for attempt in range(attempts):
        try:
            with open_url(request,timeout=90) as response:
                return response.read(max_bytes)
        except (TimeoutError, OSError, urllib.error.URLError) as error:
            last_error=error
            if attempt+1<attempts:
                time.sleep(2**attempt)
    raise last_error

def check(project, bestiary_bytes, book_bytes):
    if blob_sha(bestiary_bytes)!=BESTIARY_SHA:raise ValueError("XMM_BESTIARY_GIT_BLOB_MISMATCH")
    if blob_sha(book_bytes)!=BOOK_SHA:raise ValueError("XMM_BOOK_GIT_BLOB_MISMATCH")
    src=json.loads(bestiary_bytes)["monster"]
    book=json.loads(book_bytes)
    index=java_rows((project/JAVA_INDEX).read_text(encoding="utf-8"))
    compiled=java_rows((project/JAVA_RUNTIME).read_text(encoding="utf-8"))
    attack_contracts=java_rows((project/JAVA_ATTACK_CONDITIONS).read_text(encoding="utf-8"))
    bookrows=java_rows((project/JAVA_BOOK).read_text(encoding="utf-8"))
    spellrows=java_rows((project/JAVA_SPELLS).read_text(encoding="utf-8"))
    traitrows=java_rows((project/JAVA_TRAITS).read_text(encoding="utf-8"))
    save_damage_rows=java_rows((project/JAVA_SAVE_DAMAGE).read_text(encoding="utf-8"))
    if traitrows!=trait_rows(src):
        raise ValueError("XMM_178_TRAIT_SOURCE_INDEX_MISMATCH")
    expected_usage=usage_rows(src)
    if len(spellrows)!=132 or spellrows!=expected_usage:
        raise ValueError("XMM_132_SPELLCASTING_USAGE_MISMATCH")

    monster_errors,totals=check_monsters(src,index,compiled,attack_contracts)
    branch_errors,branch_totals=check_conditional_attack_damage(src,compiled)
    effect_errors,effect_count=check_unmodeled_action_effects(src,compiled)
    save_errors,save_count=check_save_damage_actions(src,save_damage_rows)
    section_errors=check_book(book,bookrows)
    if monster_errors or branch_errors or effect_errors or section_errors or save_errors:
        raise ValueError("XMM_SOURCE_MISMATCH:"+json.dumps(
            {"monsters":monster_errors[:20],"monster_count":len(monster_errors),
             "conditional_attack_damage":branch_errors[:20],"conditional_attack_damage_count":len(branch_errors),
             "unmodeled_action_effects":effect_errors[:20],"unmodeled_action_effects_count":len(effect_errors),
             "sections":section_errors[:20],"section_count":len(section_errors),
             "save_damage":save_errors[:20],"save_damage_count":len(save_errors)}))
    return {"format":"XMM_INDEPENDENT_SOURCE_MATCH_V1","source_commit":PIN,
        "bestiary_git_blob_sha1":BESTIARY_SHA,"book_git_blob_sha1":BOOK_SHA,
        "source_monsters_compared":503,"editorial_sections_compared":36,
        "recharge_actions_compared":totals["recharge_actions"],
        "spellcasting_usage_records_compared":len(spellrows),
        "spellcaster_records_compared":totals["spellcasting_monsters"],
        "grapple_escape_records_compared":totals["grapple_escape_records"],
        "grapple_reach_records_compared":totals["grapple_escape_records"],
        "source_trait_contracts_compared":len(traitrows),
        "source_trait_families_compared":len({r["kind"] for r in traitrows}),
        "source_save_damage_actions_compared":save_count,
        "source_abilities_with_unmodeled_effects_manual":effect_count,
        "source_compound_condition_actions_typed":2,
        **branch_totals,
        "source_field_and_catalog_gate_pass":True,
        "full_monster_combat_automation_certified":False}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source-dir",type=Path)
    p.add_argument("--project-root",type=Path,default=ROOT)
    p.add_argument("--json-out",type=Path)
    args=p.parse_args()
    if args.source_dir:
        best=(args.source_dir/"bestiary-xmm.json").read_bytes()
        book=(args.source_dir/"book-xmm.json").read_bytes()
    else:
        files=[fetch_source_bytes(SOURCE+name) for name in FILES]
        best,book=files
    result=check(args.project_root.resolve(),best,book)
    payload=json.dumps(result,indent=2,ensure_ascii=False)
    print(payload)
    if args.json_out:
        args.json_out.parent.mkdir(parents=True,exist_ok=True)
        args.json_out.write_text(payload+"\n",encoding="utf-8")
if __name__=="__main__":main()
