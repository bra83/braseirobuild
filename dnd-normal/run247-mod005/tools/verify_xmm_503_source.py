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
JAVA_MULTIATTACK="app/src/main/java/com/braseiro/dnd5e/Xmm2025MultiattackPlanIndex.java"
JAVA_SAVE_CONDITIONS="app/src/main/java/com/braseiro/dnd5e/Xmm2025SaveConditionPreviewEngine.java"
JAVA_RECURRING_DAMAGE="app/src/main/java/com/braseiro/dnd5e/Xmm2025RecurringDamagePreviewEngine.java"
JAVA_ZERO_HP_CONDITIONS="app/src/main/java/com/braseiro/dnd5e/Xmm2025ZeroHpConditionPreviewEngine.java"
JAVA_ATTACK_CONDITIONS="app/src/main/java/com/braseiro/dnd5e/Xmm2025AttackConditionEngine.java"
JAVA_SAVE_DAMAGE="app/src/main/java/com/braseiro/dnd5e/Xmm2025SaveDamageEngine.java"
SOURCE_TURN_START_HIT_CONDITIONS={
    ("Assassin","Shortsword"): ("poisoned","until the start of the assassin's next turn"),
    ("Bone Devil","Infernal Sting"): ("poisoned","until the start of the devil's next turn"),
    ("Dire Worg","Bite"): ("poisoned","until the start of the worg's next turn"),
    ("Giant Centipede","Bite"): ("poisoned","until the start of the centipede's next turn"),
    ("Centaur Warden","Sun Ray"): ("blinded","until the start of the centaur's next turn"),
    ("Ettercap","Bite"): ("poisoned","until the start of the ettercap's next turn"),
    ("Githyanki Dracomancer","Draconic Strike"): ("frightened","until the start of the githyanki's next turn"),
    ("Gnoll Fang of Yeenoghu","Bite"): ("poisoned","until the start of the gnoll's next turn"),
    ("Lich","Paralyzing Touch"): ("paralyzed","until the start of the lich's next turn"),
    ("Noble Prodigy","Beguiling Strike"): ("charmed","until the start of the noble's next turn"),
    ("Oni","Nightmare Ray"): ("frightened","until the start of the oni's next turn"),
    ("Quasit","Rend"): ("poisoned","until the start of the quasit's next turn"),
    ("Satyr Revelmaster","Prance"): ("charmed","until the start of the satyr's next turn"),
    ("Sprite","Enchanting Bow"): ("charmed","until the start of the sprite's next turn"),
    ("Vampire Umbral Lord","Sickening Ray"): ("poisoned","until the start of the vampire's next turn"),
    ("Wyvern","Sting"): ("poisoned","until the start of the wyvern's next turn"),
}
SOURCE_TURN_END_HIT_CONDITIONS={
    ("Couatl","Bite"): ("poisoned","until the end of the couatl's next turn"),
}
SOURCE_TURN_START_SAVE_CONDITIONS={
    ("Aberrant Cultist","Mind Rot"): ("poisoned","wis",15,"6d8","psychic",90,
        "until the start of the cultist's next turn"),
    ("Guardian Naga","Poisonous Spittle"): ("blinded","con",16,"7d8","poison",60,
        "until the start of the naga's next turn"),
    ("Pirate","Enthralling Panache"): ("charmed","wis",12,"", "",30,
        "until the start of the pirate's next turn"),
}
SOURCE_CONDITION_RECOVERY_ACTIONS={
    ("Myconid Sovereign","Pacifying Spores"): ("stunned","con",12,10,10,()),
    ("Myconid Adult","Pacifying Spores (1/Day)"): ("stunned","con",11,10,10,()),
    ("Quasit","Scare (1/Day)"): ("frightened","wis",10,20,10,()),
    ("Carrion Crawler","Paralyzing Tentacles"): ("poisoned","con",12,10,10,("paralyzed",)),
    ("Doppelganger","Unsettling Visage"): ("frightened","wis",12,15,10,()),
    ("Modron Pentadrone","Paralysis Gas"): ("paralyzed","con",11,30,10,()),
    ("Graveyard Revenant","Haunting Glare"): ("paralyzed","wis",15,30,10,()),
}
SOURCE_SECONDARY_ATTACK_DAMAGE={
    ("Half-Dragon","Claw"),("Vine Blight","Constricting Vine"),
}
ADVANTAGE_BONUS_ATTACK_DAMAGE={
    ("Chimera","Bite"): ("2d6 + 4",11,7,"2d6","piercing",7,"m",5,0,0),
    ("Goblin Boss","Scimitar"): ("1d6 + 2",5,2,"1d4","slashing",4,"m",5,0,0),
    ("Goblin Boss","Shortbow"): ("1d6 + 2",5,2,"1d4","piercing",4,"r",0,80,320),
    ("Goblin Warrior","Scimitar"): ("1d6 + 2",5,2,"1d4","slashing",4,"m",5,0,0),
    ("Goblin Warrior","Shortbow"): ("1d6 + 2",5,2,"1d4","piercing",4,"r",0,80,320),
    ("Scout Captain","Shortsword"): ("1d6 + 3",6,10,"3d6","piercing",5,"m",5,0,0),
    ("Scout Captain","Longbow"): ("1d8 + 3",7,10,"3d6","piercing",5,"r",0,150,600),
}
WOUNDED_TARGET_ADVANTAGE_ATTACKS={
    ("Giant Shark","Bite"): (9,"3d10 + 6","piercing"),
    ("Hunter Shark","Bite"): (6,"3d6 + 4","piercing"),
    ("Piranha","Bite"): (5,"1","piercing"),
    ("Swarm of Piranhas","Bites"): (5,"2d4 + 3","piercing"),
}
GRAPPLED_TARGET_ADVANTAGE_ATTACKS={
    ("Ankheg","Bite"): (5,"2d6 + 3 + 1d6","slashing + acid"),
    ("Bugbear Warrior","Light Hammer"): (4,"3d4 + 2","bludgeoning"),
    ("Bugbear Stalker","Morningstar"): (5,"2d8 + 3","piercing"),
    ("Mimic","Bite"): (5,"1d8 + 3 + 1d8","piercing + acid"),
}
GRAPPLE_ESCAPE_DISADVANTAGE_ACTIONS={("Mimic","Pseudopod")}
GRAPPLE_LINKED_RESTRAINT_ACTIONS={("Aberrant Cultist","Tentacle Lash"),("Chain Devil","Chain"),
    ("Crocodile","Bite"),("Giant Octopus","Tentacles"),("Mezzoloth","Claws"),
    ("Purple Worm","Bite"),("Remorhaz","Bite"),("Roc","Talons"),("Water Weird","Surge"),
    ("Giant Crocodile","Bite"),("Kraken","Tentacle"),("Tyrannosaurus Rex","Bite"),
    ("Behir","Constrict"),("Couatl","Constrict"),("Marilith","Constrict"),
    ("Salamander","Constrict"),("Yuan-ti Abomination","Constrict"),
    ("Yuan-ti Malison (Type 3)","Constrict")}
XMM_SAVE_LINKED_RESTRAINT_ACTIONS={("Behir","Constrict"),("Couatl","Constrict"),
    ("Marilith","Constrict"),("Salamander","Constrict"),
    ("Yuan-ti Abomination","Constrict"),("Yuan-ti Malison (Type 3)","Constrict")}
XMM_GRAPPLE_LINKED_CONTRACT_SIZES={
    ("Aberrant Cultist","Tentacle Lash"):"large",("Chain Devil","Chain"):"large",
    ("Crocodile","Bite"):"medium",
    ("Giant Octopus","Tentacles"):"medium",("Mezzoloth","Claws"):"large",
    ("Purple Worm","Bite"):"large",("Remorhaz","Bite"):"large",
    ("Roc","Talons"):"huge",("Water Weird","Surge"):"medium",
    ("Behir","Constrict"):"large",("Couatl","Constrict"):"medium",
    ("Marilith","Constrict"):"medium",("Salamander","Constrict"):"large",
    ("Yuan-ti Abomination","Constrict"):"large",
    ("Yuan-ti Malison (Type 3)","Constrict"):"medium",
    ("Giant Crocodile","Bite"):"large",("Kraken","Tentacle"):"gargantuan",
    ("Tyrannosaurus Rex","Bite"):"large"}
XMM_GRAPPLE_LINKED_CONTRACT_DCS={
    ("Aberrant Cultist","Tentacle Lash"):14,("Chain Devil","Chain"):14,
    ("Crocodile","Bite"):12,
    ("Giant Octopus","Tentacles"):13,("Mezzoloth","Claws"):14,
    ("Purple Worm","Bite"):19,("Remorhaz","Bite"):17,
    ("Roc","Talons"):19,("Water Weird","Surge"):13,
    ("Behir","Constrict"):16,("Couatl","Constrict"):13,
    ("Marilith","Constrict"):14,("Salamander","Constrict"):14,
    ("Yuan-ti Abomination","Constrict"):14,
    ("Yuan-ti Malison (Type 3)","Constrict"):13,
    ("Giant Crocodile","Bite"):15,("Kraken","Tentacle"):20,
    ("Tyrannosaurus Rex","Bite"):17}
XMM_GRAPPLE_LINKED_CAPACITY={
    ("Aberrant Cultist","Tentacle Lash"):2,("Chain Devil","Chain"):2,
    ("Crocodile","Bite"):1,("Giant Octopus","Tentacles"):8,("Mezzoloth","Claws"):2,
    ("Purple Worm","Bite"):1,("Remorhaz","Bite"):1,("Roc","Talons"):1,
    ("Water Weird","Surge"):1,("Giant Crocodile","Bite"):1,
    ("Kraken","Tentacle"):10,("Tyrannosaurus Rex","Bite"):1}
XMM_GRAPPLE_TAIL_EXCLUSIONS={
    ("Giant Crocodile","Bite"):"the crocodile's tail",
    ("Tyrannosaurus Rex","Bite"):"the tyrannosaurus's tail",
}
SOURCE_TURN_START_HEALING_SUPPRESSION={("Bearded Devil","Beard"),
    ("Bone Devil","Infernal Sting"),("Dire Worg","Bite")}
TARGET_TURN_END_HIT_CONDITIONS={
    ("Giant Vulture","Gouge"):"poisoned",
    ("Gas Spore Fungus","Tendril"):"poisoned",
    ("Cloud Giant","Thundercloud"):"incapacitated",
    ("Hill Giant","Trash Lob"):"poisoned",
    ("Ghast Gravecaller","Horrific Necrosis"):"frightened",
}
CHARGE_EXTRA_DAMAGE_ACTIONS={
    ("Boar","Gore"): (20,"piercing","medium"),
    ("Elk","Ram"): (20,"bludgeoning","large"),
    ("Galeb Duhr","Avalanche Slam"): (20,"bludgeoning","large"),
    ("Giant Boar","Gore"): (20,"piercing","large"),
    ("Giant Elk","Ram"): (20,"bludgeoning","huge"),
    ("Giant Goat","Ram"): (20,"bludgeoning","large"),
    ("Minotaur of Baphomet","Gore"): (10,"piercing","large"),
    ("Minotaur Skeleton","Gore"): (20,"piercing","large"),
    ("Primeval Owlbear","Ravage"): (20,"slashing","huge"),
    ("Rhinoceros","Gore"): (20,"piercing","large"),
    ("Triceratops","Gore"): (20,"piercing","huge"),
    ("Warhorse","Hooves"): (20,"bludgeoning","large"),
    ("Wereboar","Tusk (Boar or Hybrid Form Only)"): (20,"piercing","medium"),
}
MOVEMENT_ALTERNATE_ATTACK_DAMAGE={
    ("Aarakocra Skirmisher","Talons"): (30,"1d4 + 2","3d4 + 2","slashing"),
    ("Giant Seahorse","Ram"): (20,"2d6 + 2","2d8 + 2","bludgeoning"),
    ("Goat","Ram"): (20,"1","1d4","bludgeoning"),
}
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

def check_grapple_escape_riders(source, compiled, contracts, expected_count=43):
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
        save_linked=(monster,rider["name"]) in XMM_SAVE_LINKED_RESTRAINT_ACTIONS
        source_range=re.search(r"within\s+(\d+)\s+feet",text,re.I) if save_linked else None
        if "{@condition Grappled" not in text or ("escape {@dc %d}"%dc) not in text or \
           (not save_linked and ("saving throw" in text.lower() or "escape" not in text.lower() or not source_reach)) or \
           (save_linked and (not source_range or "{@condition Restrained" not in text)):
            problems.append(monster+":"+rider["name"]+":PINNED_ESCAPE_SOURCE_CHANGED")
            continue
        source_saves=re.findall(r"\{@actSave\s+(str|dex|con|int|wis|cha)\}\s*\{@dc\s*(\d+)",text,re.I)
        save_mismatch=(bool(source_saves) and (len(source_saves)!=1 or
            dst.get("save_ability","").lower()!=source_saves[0][0].lower() or
            dst.get("save_dc")!=int(source_saves[0][1]) or not dst.get("condition_on_fail")))
        save_unproven=(not source_saves and any(k in dst for k in ("save_dc","save_ability","condition_on_fail")))
        if save_linked and dst.get("target_range_ft")!=int(source_range.group(1)):
            problems.append(monster+":"+rider["name"]+":GRAPPLE_RANGE_MISMATCH")
            continue
        if not save_linked and dst.get("reach_ft")!=int(source_reach.group(1)):
            problems.append(monster+":"+rider["name"]+":GRAPPLE_REACH_MISMATCH")
            continue
        if rider.get("choice_required",False):
            condition_ok=(dst.get("condition_alternative")=="grappled" and
                dst.get("manual_effect_choice_required") is True and
                not dst.get("condition_on_hit"))
        elif (monster,rider["name"]) in XMM_SAVE_LINKED_RESTRAINT_ACTIONS:
            condition_ok=(dst.get("condition_on_fail")=="grappled, restrained" and
                dst.get("linked_condition_bundle_until_grapple_ends") is True)
        else:
            condition_ok=("grappled" in dst.get("condition_on_hit","").lower())
        typed_escape_disadvantage=(monster,rider["name"]) in GRAPPLE_ESCAPE_DISADVANTAGE_ACTIONS and \
            "ability checks made to escape this grapple have {@variantrule disadvantage|xphb}" in text.casefold() and \
            dst.get("escape_check_disadvantage") is True
        typed_linked_conditions=(monster,rider["name"]) in GRAPPLE_LINKED_RESTRAINT_ACTIONS and \
            source_action_has_typed_condition_contract(monster,src,dst)
        semantic_ok=(dst.get("escape_dc")==dc and
            dst.get("condition_max_target_size")==rider.get("size","").lower() and
            condition_ok and not save_mismatch and not save_unproven and
            ((monster,rider["name"]) not in GRAPPLE_ESCAPE_DISADVANTAGE_ACTIONS or typed_escape_disadvantage) and
            ((monster,rider["name"]) not in GRAPPLE_LINKED_RESTRAINT_ACTIONS or typed_linked_conditions))
        manual_ok=(bool(dst.get("manual_rider_resolution_required",False))==
            bool(rider.get("other",False) and not (typed_escape_disadvantage or typed_linked_conditions)))
        if not semantic_ok:
            problems.append(monster+":"+rider["name"]+":ESCAPE_DC_MISCLASSIFIED")
        if not manual_ok:
            problems.append(monster+":"+rider["name"]+":MANUAL_RIDER_FLAG_MISMATCH")
        if semantic_ok and manual_ok:
            checked+=1
    # Save-based constrictions are grapple escape records, but they are not
    # attack-hit riders and therefore live outside the attack condition index.
    # Keep their source binding in this verifier without routing them through
    # the attack-only resolver.
    for monster,name in sorted(XMM_SAVE_LINKED_RESTRAINT_ACTIONS):
        source_row=next((x for x in source if x.get("name")==monster),None)
        if source_row is None:
            continue
        compiled_row=next((x for x in compiled if x.get("id")==monster),None)
        src=next((x for x in (source_row or {}).get("action",[]) if x.get("name")==name),None)
        dst=next((x for x in (compiled_row or {}).get("actions",[]) if x.get("name")==name),None)
        text=json.dumps((src or {}).get("entries",[]),ensure_ascii=False)
        escape=re.search(r"escape\s+\{@dc\s+(\d+)\}",text,re.I)
        size=re.search(r"one\s+(Tiny|Small|Medium|Large|Huge|Gargantuan)\s+or\s+smaller creature",text,re.I)
        reach=re.search(r"within\s+(\d+)\s+feet",text,re.I)
        saves=re.findall(r"\{@actSave\s+(str|dex|con|int|wis|cha)\}\s*\{@dc\s*(\d+)",text,re.I)
        valid=(src is not None and dst is not None and escape is not None and size is not None and
            reach is not None and len(saves)==1 and
            "{@condition Grappled" in text and "{@condition Restrained" in text and
            dst.get("escape_dc")==int(escape.group(1)) and
            dst.get("condition_max_target_size")==size.group(1).lower() and
            dst.get("target_range_ft")==int(reach.group(1)) and
            dst.get("save_ability","").lower()==saves[0][0].lower() and
            dst.get("save_dc")==int(saves[0][1]) and
            dst.get("condition_on_fail")=="grappled, restrained" and
            dst.get("linked_condition_bundle_until_grapple_ends") is True and
            dst.get("manual_rider_resolution_required") is False)
        if valid:checked+=1
        else:problems.append(monster+":"+name+":SAVE_LINKED_GRAPPLE_SOURCE_MISMATCH")
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
        fixed_sizes={"Balor":"Huge","Colossus":"Gargantuan","Merrow":"Large",
                     "Satyr":"Medium","Shambling Mound":"Large",
                     "Giant Crocodile":"Huge","Kraken":"Gargantuan",
                     "Tyrannosaurus Rex":"Huge","Tarrasque":"Gargantuan","Myconid Sovereign":"Large",
                     "Myconid Adult":"Medium","Quasit":"Tiny",
                     "Behir":"Huge","Couatl":"Medium","Marilith":"Large",
                     "Salamander":"Large","Yuan-ti Abomination":"Large",
                     "Yuan-ti Malison (Type 3)":"Medium"}
        if monster in fixed_sizes:
            code={"T":"Tiny","S":"Small","M":"Medium","L":"Large","H":"Huge","G":"Gargantuan"}
            source_sizes=[code.get(x,x) for x in s.get("size",[])]
            if source_sizes!=[fixed_sizes[monster]] or c.get("size_category")!=fixed_sizes[monster]:
                problems.append(monster+":SIZE_CATEGORY")
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

ATTACK_DAMAGE_TYPES=("bludgeoning|piercing|slashing|acid|cold|fire|force|lightning|"
                     "necrotic|poison|psychic|radiant|thunder")
ATTACK_DAMAGE_COMPONENT=re.compile(
    r"\{@damage\s+([^}]+)\}\)\s*("+ATTACK_DAMAGE_TYPES+r")\s+damage",re.I)
def check_attack_primary_damage(source,compiled,expected_count=581):
    """Compare the immediate hit damage sentence for simple typed attacks.

    Recurring damage and later save effects are intentionally checked as
    separate source-bound riders; they must never be folded into hit damage.
    Conditional alternatives remain under their dedicated contracts.
    """
    by_id={row["id"]:row for row in compiled};errors=[];checked=0
    for monster in source:
        target=by_id.get(monster.get("name"),{})
        for slot,action in enumerate(monster.get("action",[])):
            entries=action.get("entries",[])
            text=" ".join(value for value in entries if isinstance(value,str))
            marker=text.find("{@h}")
            if marker<0:continue
            sentence_end=text.find(".",marker)
            segment=text[marker:sentence_end if sentence_end>=0 else len(text)]
            components=ATTACK_DAMAGE_COMPONENT.findall(segment)
            if not components:continue
            compiled_actions=target.get("actions",[])
            record=compiled_actions[slot] if slot<len(compiled_actions) else {}
            if (re.search(r"\bor\b|\bif\b|\bunless\b|type chosen",segment,re.I) or
                record.get("damage_alternative_expression") or record.get("extra_damage_condition")):
                continue
            checked+=1
            expected_damage=" + ".join(expr.strip() for expr,_ in components)
            expected_type=" + ".join(kind.lower() for _,kind in components)
            if (record.get("damage","").replace(" ","")!=expected_damage.replace(" ","") or
                record.get("damage_type","").lower()!=expected_type):
                errors.append(f"{monster['name']}:{action.get('name','?')}:PRIMARY_HIT_DAMAGE_MISMATCH")
    if checked!=expected_count:errors.append("ATTACK_PRIMARY_DAMAGE_RECORD_COUNT:"+str(checked))
    return errors,checked

def check_attack_secondary_damage_contracts(source,compiled):
    """Pin six source riders whose ongoing/area damage was once folded into hit damage."""
    expected={
      ("Animated Rug of Smothering","Smother"):("2d6 + 3","bludgeoning"),
      ("Dao","Earth Burst"):("2d8 + 6","bludgeoning"),
      ("Half-Dragon","Claw"):("1d4 + 4","slashing"),
      ("Pit Fiend","Bite"):("3d6 + 8","piercing"),
      ("Stirge","Proboscis"):("1d6 + 3","piercing"),
      ("Vine Blight","Constricting Vine"):("1d8 + 2","bludgeoning"),
    }
    source_by_id={m.get("name"):m for m in source};compiled_by_id={m.get("id"):m for m in compiled}
    errors=[];checked=0
    for (monster,name),(damage,damage_type) in expected.items():
        src=source_by_id.get(monster);dst=compiled_by_id.get(monster)
        action=next((a for a in (src or {}).get("action",[]) if a.get("name")==name),None)
        record=next((a for a in (dst or {}).get("actions",[]) if a.get("name")==name),None)
        if action is None or record is None:
            errors.append(f"{monster}:{name}:SECONDARY_DAMAGE_RECORD_MISSING");continue
        text=json.dumps(action.get("entries",[]),ensure_ascii=False).casefold()
        if record.get("damage")!=damage or record.get("damage_type","").lower()!=damage_type:
            errors.append(f"{monster}:{name}:SECONDARY_DAMAGE_FOLDED_INTO_HIT")
        if not record.get("manual_rider_resolution_required",False):
            errors.append(f"{monster}:{name}:SECONDARY_DAMAGE_NOT_MANUAL")
        signatures={
          ("Animated Rug of Smothering","Smother"):"instead of dealing damage",
          ("Dao","Earth Burst"):"earth explodes from the target's space",
          ("Half-Dragon","Claw"):"type chosen for the draconic origin trait",
          ("Pit Fiend","Bite"):"at the start of each of its turns",
          ("Stirge","Proboscis"):"at the start of each of the stirge's turns",
          ("Vine Blight","Constricting Vine"):"at the start of each of its turns",
        }
        if signatures[(monster,name)] not in text:
            errors.append(f"{monster}:{name}:PINNED_SECONDARY_SOURCE_CLAUSE_MISSING")
        if monster=="Animated Rug of Smothering" and (
            record.get("condition_on_hit") or record.get("condition_alternative")!="grappled" or
            not record.get("manual_effect_choice_required")):
            errors.append(f"{monster}:{name}:OPTIONAL_GRAPPLE_MISREPRESENTED_AS_AUTOMATIC")
        if monster=="Dao" and (record.get("secondary_save_damage")!="3d6" or
            record.get("secondary_save_damage_type")!="thunder" or
            record.get("secondary_save_area_ft")!=10):
            errors.append(f"{monster}:{name}:AREA_SAVE_DAMAGE_NOT_SEPARATE")
        if monster=="Half-Dragon" and (record.get("extra_damage")!="2d6" or
            record.get("extra_damage_type")!="draconic_origin"):
            errors.append(f"{monster}:{name}:DRACONIC_ORIGIN_DAMAGE_NOT_TYPED")
        if monster=="Pit Fiend" and (record.get("condition_recurring_damage")!="6d6" or
            record.get("condition_recurring_damage_type")!="poison" or
            record.get("condition_recurring_damage_tick")!="target_turn_start"):
            errors.append(f"{monster}:{name}:POISON_TURN_DAMAGE_NOT_SEPARATE")
        if monster=="Stirge" and (record.get("condition_recurring_damage")!="2d4" or
            record.get("condition_recurring_damage_type")!="necrotic" or
            record.get("condition_recurring_damage_tick")!="source_turn_start"):
            errors.append(f"{monster}:{name}:ATTACH_DAMAGE_NOT_SEPARATE")
        if monster=="Vine Blight" and (record.get("condition_recurring_damage")!="1d8" or
            record.get("condition_recurring_damage_type")!="bludgeoning" or
            record.get("condition_recurring_damage_tick")!="target_turn_start"):
            errors.append(f"{monster}:{name}:GRAPPLE_DAMAGE_NOT_SEPARATE")
        checked+=1
    if checked!=len(expected):errors.append("ATTACK_SECONDARY_DAMAGE_RECORD_COUNT:"+str(checked))
    return errors,checked

def peryton_gore_extra_damage_matches(source_action,compiled_action):
    """Keep the Peryton's movement rider additive and manually gated."""
    entry=" ".join(x for x in source_action.get("entries",[]) if isinstance(x,str))
    source_contract=re.search(
        r"1d8\s*\+\s*3\}\)\s+Piercing damage\. If the peryton moved 30\+ feet straight toward the target immediately before the hit, the target takes an extra 9\s*\(\{@damage\s+2d8\}\) Piercing damage",
        entry,re.I)
    return bool(source_contract and
        compiled_action.get("damage")=="1d8 + 3" and
        compiled_action.get("damage_type","").lower()=="piercing" and
        compiled_action.get("extra_damage")=="2d8" and
        compiled_action.get("extra_damage_type","").lower()=="piercing" and
        compiled_action.get("extra_damage_condition")=="moved_30ft_straight_toward_target_immediately_before_hit" and
        not compiled_action.get("damage_alternative_expression") and
        not compiled_action.get("damage_alternative_condition") and
        compiled_action.get("manual_rider_resolution_required") is True)

def charge_extra_damage_matches(monster_name,source_action,compiled_action):
    action_name=RCH.sub("",source_action.get("name","")).strip()
    contract=CHARGE_EXTRA_DAMAGE_ACTIONS.get((monster_name,action_name))
    if contract is None:
        return True
    distance,damage_type,size=contract
    entry=" ".join(x for x in source_action.get("entries",[]) if isinstance(x,str))
    before,separator,after=entry.partition(" If ")
    base_components=re.findall(r"\{@damage\s+([^}]+)\}\)\s*([A-Za-z]+)\s+damage",before,re.I)
    extra=re.search(r"takes an extra \d+\s*\(\{@damage\s+([^}]+)\}\)\s*([A-Za-z]+)\s+damage",after,re.I)
    source_distance=re.search(r"moved (\d+)\+ feet straight toward",after,re.I)
    source_size=re.search(r"target is (?:a )?(tiny|small|medium|large|huge|gargantuan) or smaller",after,re.I)
    expected_condition=f"moved_{distance}ft_straight_toward_target_immediately_before_hit"
    return bool(separator and len(base_components)>=1 and extra and source_distance and source_size and
        int(source_distance.group(1))==distance and source_size.group(1).lower()==size and
        any("{@condition Prone" in x for x in source_action.get("entries",[]) if isinstance(x,str)) and
        compiled_action.get("damage")==" + ".join(x[0].strip() for x in base_components) and
        compiled_action.get("damage_type","").lower()==" + ".join(x[1].lower() for x in base_components) and
        compiled_action.get("extra_damage")==extra.group(1).strip() and
        extra.group(2).lower()==damage_type and
        compiled_action.get("extra_damage_type","").lower()==damage_type and
        compiled_action.get("extra_damage_condition")==expected_condition and
        compiled_action.get("extra_damage_condition_on_hit")=="prone" and
        compiled_action.get("extra_damage_max_target_size")==size and
        "condition_on_fail" not in compiled_action and
        compiled_action.get("manual_rider_resolution_required") is True)

def movement_alternate_attack_matches(monster_name,source_action,compiled_action):
    action_name=RCH.sub("",source_action.get("name","")).strip()
    contract=MOVEMENT_ALTERNATE_ATTACK_DAMAGE.get((monster_name,action_name))
    if contract is None:return True
    distance,base,alternate,damage_type=contract
    entry=" ".join(x for x in source_action.get("entries",[]) if isinstance(x,str))
    movement=re.search(r"moved (\d+)\+ feet straight toward the target immediately before the hit",entry,re.I)
    base_source=("{@damage "+base+"}" in entry if base!="1" else
        bool(re.search(r"\{@h\}\s*1\s+bludgeoning damage",entry,re.I)))
    alternate_source="{@damage "+alternate+"}" in entry
    expected_condition=f"moved_{distance}ft_straight_toward_target_immediately_before_hit"
    return (movement is not None and int(movement.group(1))==distance and
        base_source and alternate_source and
        compiled_action.get("damage")==base and
        compiled_action.get("damage_alternative_expression")==alternate and
        compiled_action.get("damage_alternative_condition")==expected_condition and
        compiled_action.get("damage_type","").lower()==damage_type and
        not compiled_action.get("extra_damage") and
        not compiled_action.get("manual_rider_resolution_required",False) and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def source_action_has_saving_throw(action):
    text=json.dumps(action,ensure_ascii=False)
    return bool(re.search(r"\{@actSave\s+(?:str|dex|con|int|wis|cha)\}|saving throws?|spell saves?",text,re.I))

def condition_on_fail_matches_source(action,compiled_action):
    return (not compiled_action.get("condition_on_fail") or source_action_has_saving_throw(action))

def check_conditional_attack_damage(source,compiled,expected_bloodied=13,expected_manual=0):
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
            if not condition_on_fail_matches_source(action,compiled_action):
                errors.append(f"{monster['name']}:{action['name']}:CONDITION_ON_FAIL_WITHOUT_SOURCE_SAVE")
            if compiled_action.get("save_dc",0) and not source_action_has_saving_throw(action):
                errors.append(f"{monster['name']}:{action['name']}:SAVE_DC_WITHOUT_SOURCE_SAVING_THROW")
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
                typed_wounded_advantage=(monster["name"],RCH.sub("",action.get("name","" )).strip()) in WOUNDED_TARGET_ADVANTAGE_ATTACKS and wounded_target_advantage_matches(
                    monster["name"],action,compiled_action)
                source_requires_manual=("{@condition Grappled" in entry or
                    ("{@variantrule Advantage" in entry and not typed_wounded_advantage))
                if bool(compiled_action.get("manual_rider_resolution_required",False))!=source_requires_manual:
                    errors.append(f"{monster['name']}:{action['name']}:BLOODIED_OTHER_RIDER_MANUAL_FLAG")
            elif alt:
                if monster["name"]=="Chimera" and action.get("name")=="Bite":
                    if (compiled_action.get("damage")!="2d6 + 4" or
                        compiled_action.get("damage_type")!="piercing" or
                        compiled_action.get("extra_damage_if_attack_advantage")!="2d6" or
                        compiled_action.get("extra_damage_if_attack_advantage_type")!="piercing" or
                        compiled_action.get("manual_rider_resolution_required",False)):
                        errors.append("Chimera:Bite:ADVANTAGE_DAMAGE_NOT_EXECUTABLE")
                elif monster["name"]=="Mimic" and action.get("name")=="Bite":
                    if not grappled_target_advantage_matches("Mimic",action,compiled_action):
                        errors.append("Mimic:Bite:GRAPPLED_ADVANTAGE_DAMAGE_NOT_EXECUTABLE")
                elif (monster["name"],RCH.sub("",action.get("name","" )).strip()) in MOVEMENT_ALTERNATE_ATTACK_DAMAGE:
                    if not movement_alternate_attack_matches(monster["name"],action,compiled_action):
                        errors.append(f"{monster['name']}:{action['name']}:MOVEMENT_ALTERNATE_DAMAGE_NOT_EXECUTABLE")
                else:
                    manual_count+=1
                    if not compiled_action.get("manual_rider_resolution_required",False):
                        errors.append(f"{monster['name']}:{action['name']}:UNMODELED_ALTERNATE_DAMAGE_NOT_MANUAL")
                if monster["name"]=="Mimic" and action.get("name")=="Bite":
                    typed_damage=re.findall(r"\{@damage\s+([^}]+)\}\)\s*([A-Za-z]+)\s+damage",entry,re.I)
                    if len(typed_damage)!=3:
                        errors.append("Mimic:Bite:GRAPPLE_BRANCH_SOURCE_SHAPE")
                    else:
                        base="{} + {}".format(typed_damage[0][0].strip(),typed_damage[2][0].strip())
                        damage_type="{} + {}".format(typed_damage[0][1].lower(),typed_damage[2][1].lower())
                        if compiled_action.get("damage")!=base:
                            errors.append(f"Mimic:Bite:BASE_DAMAGE:{compiled_action.get('damage')}!={base}")
                        if compiled_action.get("damage_type","").lower()!=damage_type:
                            errors.append(f"Mimic:Bite:BASE_DAMAGE_TYPE:{compiled_action.get('damage_type')}!={damage_type}")
                        if compiled_action.get("condition_on_fail") or compiled_action.get("condition_on_hit"):
                            errors.append("Mimic:Bite:GRAPPLED_PREREQUISITE_MISMODELED_AS_RIDER")
            if re.search(r"\bif\b.{0,220}\b(?:takes|deals) an extra\s+\d+\s*\(\{@damage",entry,re.I) and \
               not compiled_action.get("manual_rider_resolution_required",False):
                errors.append(f"{monster['name']}:{action['name']}:CONDITIONAL_EXTRA_DAMAGE_NOT_MANUAL")
            if monster["name"]=="Peryton" and action.get("name")=="Gore" and \
               not peryton_gore_extra_damage_matches(action,compiled_action):
                errors.append("Peryton:Gore:EXTRA_DAMAGE_BRANCH_MISMATCH")
            if (monster["name"],RCH.sub("",action.get("name","")).strip()) in CHARGE_EXTRA_DAMAGE_ACTIONS and \
               not charge_extra_damage_matches(monster["name"],action,compiled_action):
                errors.append(f"{monster['name']}:{action['name']}:CHARGE_EXTRA_DAMAGE_BRANCH_MISMATCH")
            if (monster["name"],RCH.sub("",action.get("name","")).strip()) in MOVEMENT_ALTERNATE_ATTACK_DAMAGE and \
               not movement_alternate_attack_matches(monster["name"],action,compiled_action):
                errors.append(f"{monster['name']}:{action['name']}:MOVEMENT_ALTERNATE_DAMAGE_BRANCH_MISMATCH")
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

def advantage_bonus_attack_matches(monster_name,action,compiled_action):
    """Bind the six source attacks whose bonus damage requires attack advantage."""
    name=RCH.sub("",action.get("name","" )).strip()
    expected=ADVANTAGE_BONUS_ATTACK_DAMAGE.get((monster_name,name))
    if expected is None:return False
    if (monster_name,name)==("Chimera","Bite"):
        text=" ".join(x for x in action.get("entries",[]) if isinstance(x,str))
        source=("{@atkr m} {@hit 7}" in text and "reach 5 ft." in text and
            "{@h}11 ({@damage 2d6 + 4}) Piercing damage" in text and
            "or 18 ({@damage 4d6 + 4}) Piercing damage if the chimera had {@variantrule Advantage|XPHB}" in text)
        return (source and compiled_action.get("attack_modes")==["m"] and
            compiled_action.get("to_hit")==7 and compiled_action.get("reach_ft")==5 and
            compiled_action.get("damage")=="2d6 + 4" and compiled_action.get("damage_type")=="piercing" and
            compiled_action.get("extra_damage_if_attack_advantage")=="2d6" and
            compiled_action.get("extra_damage_if_attack_advantage_type")=="piercing" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    base,base_average,bonus_average,bonus,damage_type,to_hit,mode,reach,normal,long_range=expected
    entries=action.get("entries",[])
    text=" ".join(x for x in entries if isinstance(x,str))
    marker=text.find("{@h}")
    end=text.find(".",marker) if marker>=0 else -1
    segment=text[marker:end if end>=0 else len(text)] if marker>=0 else ""
    components=ATTACK_DAMAGE_COMPONENT.findall(segment)
    dice=[x.strip() for x,_ in components]
    types=[x.lower() for _,x in components]
    base_match=re.search(r"\{@h\}\s*"+str(base_average)+r"\s*\(\{@damage\s+"+
        re.escape(base)+r"\}\)\s*"+re.escape(damage_type)+r"\s+damage",text,re.I)
    source_advantage=bool(re.search(
        r"plus\s+"+str(bonus_average)+r"\s*\(\{@damage\s+"+re.escape(bonus)+
        r"\}\)\s+"+re.escape(damage_type)+
        r"\s+damage\s+if the attack (?:roll had|was made with) "+
        r"\{@variantrule Advantage\|XPHB\}",text,re.I))
    source_hit=re.search(r"\{@hit\s+(\d+)\}",text,re.I)
    source_modes=re.search(r"\{@atkr\s+([mr,]+)\}",text,re.I)
    source_reach=re.search(r"reach\s+(\d+)\s*ft\.",text,re.I)
    source_range=re.search(r"range\s+(\d+)/(\d+)\s*ft\.",text,re.I)
    compiled_modes=compiled_action.get("attack_modes",[])
    range_ok=(source_reach is not None and int(source_reach.group(1))==reach and
        compiled_action.get("reach_ft")==reach and source_range is None) if reach else (
        source_range is not None and
        (int(source_range.group(1)),int(source_range.group(2)))==(normal,long_range) and
        compiled_action.get("range_normal_ft")==normal and compiled_action.get("range_long_ft")==long_range and
        source_reach is None)
    return (dice==[base,bonus] and types==[damage_type,damage_type] and base_match is not None and
        source_hit is not None and int(source_hit.group(1))==to_hit and
        source_modes is not None and source_modes.group(1)==mode and compiled_modes==[mode] and
        range_ok and source_advantage and
        compiled_action.get("damage")==base and
        compiled_action.get("damage_type")==damage_type and
        compiled_action.get("extra_damage_if_attack_advantage")==bonus and
        compiled_action.get("extra_damage_if_attack_advantage_type")==damage_type and
        not compiled_action.get("manual_rider_resolution_required",False) and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def simple_multiattack_plan(action):
    """Parse only an exact attack-only source sentence with unrestricted choices."""
    entries=action.get("entries",[])
    text=" ".join(x for x in entries if isinstance(x,str))
    match=re.fullmatch(r"The [A-Za-z' -]+ makes (two|three|four) attacks, using (.+?) in any combination\.",text,re.I)
    if not match:return None
    if "can use" in text.casefold().split(" makes",1)[0]:return None
    if not re.search(r"\b(?:or|and)\b",match.group(2),re.I):return None
    count={"two":2,"three":3,"four":4}[match.group(1).casefold()]
    options=[x.strip() for x in re.split(r",\s*|\s+(?:or|and)\s+",match.group(2)) if x.strip()]
    options=[x[3:] if x.casefold().startswith("or ") else x for x in options]
    if len(options)<2 or any(not x for x in options):return None
    return count,"|".join(options)

def simple_repeat_multiattack_plan(monster,action):
    """Parse only a whole source sentence repeating one named attack."""
    text=" ".join(x for x in action.get("entries",[]) if isinstance(x,str))
    match=re.fullmatch(r"The [A-Za-z' -]+ makes (two|three|four) (.+?) attacks?\.",text,re.I)
    if not match:return None
    count={"two":2,"three":3,"four":4}[match.group(1).casefold()]
    phrase=match.group(2).strip().casefold()
    names=[a.get("name","") for a in monster.get("action",[]) if a.get("name")!="Multiattack"]
    normalized={phrase,phrase[:-1] if phrase.endswith("s") else phrase+"s"}
    found=[name for name in names if name.casefold() in normalized]
    return (count,found[0]) if len(found)==1 else None

def simple_ordered_multiattack_plan(monster,action):
    """Parse a whole sentence with fixed quantities of named attacks."""
    text=" ".join(x for x in action.get("entries",[]) if isinstance(x,str))
    number={"one":1,"two":2,"three":3,"four":4}
    match=re.fullmatch(r"The (?P<subject>[A-Za-z' -]+) makes (?P<body>.+)\.",text,re.I)
    if not match:return None
    if re.search(r"\b(?:uses|and|can|could|or|if|then|also|replace|replaces)\b",match.group("subject"),re.I):return None
    body=match.group("body");pos=0
    names=sorted({a.get("name","") for a in monster.get("action",[])
                  if a.get("name")!="Multiattack" and a.get("name")},key=len,reverse=True)
    sequence=[]
    while pos<len(body):
        quantity=re.match(r"(one|two|three|four)\s+",body[pos:],re.I)
        if not quantity:return None
        pos+=quantity.end()
        found=[]
        for name in names:
            variants={name,name[:-1] if name.casefold().endswith("s") else name+"s"}
            for variant in variants:
                action_name=re.match(re.escape(variant)+r"\s+attacks?\b",body[pos:],re.I)
                if action_name:found.append((name,action_name.end()))
        if len({name for name,_ in found})!=1:return None
        source_name,consumed=found[0];pos+=consumed
        sequence.extend([source_name]*number[quantity.group(1).casefold()])
        if pos==len(body):break
        separator=re.match(r",\s+and\s+|,\s+|\s+and\s+",body[pos:],re.I)
        if not separator:return None
        pos+=separator.end()
        if pos==len(body):return None
    return sequence

def check_simple_multiattacks(source,compiled):
    by_id={m.get("id"):m for m in compiled};errors=[];checked=0
    for monster in source:
        action=next((a for a in monster.get("action",[]) if a.get("name")=="Multiattack"),None)
        if action is None:continue
        choice_plan=simple_multiattack_plan(action)
        repeat_plan=simple_repeat_multiattack_plan(monster,action)
        ordered_plan=simple_ordered_multiattack_plan(monster,action)
        if choice_plan is None and repeat_plan is None and ordered_plan is None:continue
        dst=by_id.get(monster.get("name"),{})
        if choice_plan is not None:
            candidate_components=choice_plan[1].split("|")
        elif repeat_plan is not None:
            candidate_components=[repeat_plan[1]]
        else:
            candidate_components=list(dict.fromkeys(ordered_plan))
        source_actions={a.get("name"):a for a in monster.get("action",[])
                        if a.get("name")!="Multiattack"}
        runtime_actions={a.get("name"):a for a in dst.get("actions",[])}
        executable=True
        for component in candidate_components:
            source_attack=source_actions.get(component,{})
            runtime_attack=runtime_actions.get(component,{})
            source_text=" ".join(x for x in source_attack.get("entries",[]) if isinstance(x,str))
            if (not re.search(r"\{@atkr\b",source_text,re.I) or
                source_action_requires_manual_resolution(source_attack,monster.get("name","")) or
                runtime_attack.get("to_hit") is None or
                runtime_attack.get("manual_rider_resolution_required",False) or
                runtime_attack.get("manual_unmodeled_source_effect_required",False) or
                runtime_attack.get("condition_on_hit") or runtime_attack.get("condition_on_fail") or
                runtime_attack.get("condition_duration_rounds") or runtime_attack.get("condition_duration_tick") or
                runtime_attack.get("condition_ends_when") or runtime_attack.get("escape_dc") or
                runtime_attack.get("effect_on_hit") or runtime_attack.get("effect_on_fail") or
                runtime_attack.get("save_dc") or runtime_attack.get("save_ability")):
                executable=False;break
        if not executable:continue
        checked+=1
        runtime=dst.get("multiattack",{})
        runtime_action=next((a for a in dst.get("actions",[]) if a.get("name")=="Multiattack"),{})
        if choice_plan is not None:
            count,options=choice_plan
            sequence_ok=(runtime.get("options")==options and not runtime.get("sequence"))
            components=options.split("|")
        elif repeat_plan is not None:
            count,component=repeat_plan
            sequence_ok=(runtime.get("sequence")==[component]*count and not runtime.get("options"))
            components=[component]
        else:
            sequence=ordered_plan;count=len(sequence)
            sequence_ok=(runtime.get("sequence")==sequence and not runtime.get("options"))
            components=list(set(sequence))
        if (runtime.get("count")!=count or not sequence_ok or
            runtime.get("manual_sequence_required",False) or
            runtime_action.get("manual_rider_resolution_required",False)):
            errors.append(f"{monster['name']}:Multiattack:SOURCE_SEQUENCE_MISMATCH")
        actions={a.get("name"):a for a in dst.get("actions",[])}
        for component in components:
            attack=actions.get(component,{})
            if (attack.get("to_hit") is None or attack.get("manual_rider_resolution_required",False) or
                attack.get("manual_unmodeled_source_effect_required",False)):
                errors.append(f"{monster['name']}:Multiattack:COMPONENT_ATTACK_UNAVAILABLE:{component}")
    return errors,checked


def check_advantage_bonus_attack_damage(source,compiled):
    by_id={row["id"]:row for row in compiled};errors=[];checked=0
    for (monster,name),_ in ADVANTAGE_BONUS_ATTACK_DAMAGE.items():
        src=next((x for x in source if x.get("name")==monster),None)
        row=by_id.get(monster,{})
        action=next((x for x in (src or {}).get("action",[]) if RCH.sub("",x.get("name","" )).strip()==name),None)
        compiled_action=next((x for x in row.get("actions",[]) if x.get("name")==name),None)
        if action is None or compiled_action is None or not advantage_bonus_attack_matches(monster,action,compiled_action):
            errors.append(f"{monster}:{name}:ADVANTAGE_BONUS_DAMAGE_MISMATCH")
        else:checked+=1
    if checked!=len(ADVANTAGE_BONUS_ATTACK_DAMAGE):errors.append("ADVANTAGE_BONUS_ATTACK_COUNT:"+str(checked))
    return errors,checked

def wounded_target_advantage_matches(monster_name,action,compiled_action):
    name=RCH.sub("",action.get("name","" )).strip()
    expected=WOUNDED_TARGET_ADVANTAGE_ATTACKS.get((monster_name,name))
    if expected is None:return False
    to_hit,damage,damage_type=expected
    text=" ".join(x for x in action.get("entries",[]) if isinstance(x,str))
    prefix=re.search(r"\{@atkr\s+m\}\s*\{@hit\s+(\d+)\}",text,re.I)
    reach=re.search(r"reach\s+(\d+)\s*ft\.",text,re.I)
    hit=re.search(r"\{@h\}\s*(\d+)(?:\s*\(\{@damage\s+([^}]+)\}\))?\s+"+
        re.escape(damage_type)+r"\s+damage",text,re.I)
    conditional=bool(re.search(
        r"\(with\s+\{@variantrule Advantage\|XPHB\}\s+if the target doesn't have all its " +
        r"\{@variantrule Hit Points\|XPHB\}\)",text,re.I))
    return (prefix is not None and int(prefix.group(1))==to_hit and
        reach is not None and int(reach.group(1))==5 and hit is not None and
        (hit.group(2) or "1").replace(" ","")==damage.replace(" ","") and
        compiled_action.get("to_hit")==to_hit and compiled_action.get("attack_modes")==["m"] and
        compiled_action.get("reach_ft")==5 and compiled_action.get("damage")==damage and
        compiled_action.get("damage_type")==damage_type and conditional and
        compiled_action.get("attack_advantage_if_target_missing_hit_points") is True and
        (monster_name!="Swarm of Piranhas" or
         compiled_action.get("damage_bloodied")=="1d4 + 3") and
        not compiled_action.get("manual_rider_resolution_required",False) and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def check_wounded_target_advantage_attacks(source,compiled):
    by_id={row["id"]:row for row in compiled};errors=[];checked=0
    for (monster,name),_ in WOUNDED_TARGET_ADVANTAGE_ATTACKS.items():
        src=next((x for x in source if x.get("name")==monster),None);row=by_id.get(monster,{})
        action=next((x for x in (src or {}).get("action",[]) if RCH.sub("",x.get("name","" )).strip()==name),None)
        runtime=next((x for x in row.get("actions",[]) if x.get("name")==name),None)
        if action is None or runtime is None or not wounded_target_advantage_matches(monster,action,runtime):
            errors.append(f"{monster}:{name}:WOUNDED_TARGET_ADVANTAGE_MISMATCH")
        else:checked+=1
    if checked!=len(WOUNDED_TARGET_ADVANTAGE_ATTACKS):
        errors.append("WOUNDED_TARGET_ADVANTAGE_COUNT:"+str(checked))
    return errors,checked

def grappled_target_advantage_matches(monster_name,action,compiled_action):
    name=RCH.sub("",action.get("name","" )).strip()
    expected=GRAPPLED_TARGET_ADVANTAGE_ATTACKS.get((monster_name,name))
    if expected is None:return False
    to_hit,damage,damage_type=expected
    text=" ".join(x for x in action.get("entries",[]) if isinstance(x,str))
    modes="m" if monster_name in {"Ankheg","Bugbear Stalker","Mimic"} else "m,r"
    attack=bool(re.search(r"\{@atkr\s+"+modes+r"\}\s*\{@hit\s+"+str(to_hit)+r"\}",text,re.I))
    source_actor="ankheg" if monster_name=="Ankheg" else ("mimic" if monster_name=="Mimic" else "bugbear")
    conditional=bool(re.search(r"\{@variantrule Advantage\|XPHB\}\s+if the target is\s+"+
        r"\{@condition Grappled\|XPHB\}\s+by the "+re.escape(source_actor),text,re.I))
    if monster_name=="Ankheg":
        source_rider=("reach 5 ft." in text and "{@damage 2d6 + 3}" in text and
            "{@damage 1d6}" in text and "slashing damage" in text.casefold() and
            "acid damage" in text.casefold() and "large or smaller" in text.casefold() and
            "{@dc 13}" in text and "{@condition Grappled|XPHB}" in text)
        runtime_rider=(compiled_action.get("attack_modes")==["m"] and
            compiled_action.get("reach_ft")==5 and compiled_action.get("damage")==damage and
            compiled_action.get("damage_type")==damage_type and
            compiled_action.get("condition_on_hit")=="grappled" and
            compiled_action.get("condition_max_target_size")=="large" and compiled_action.get("escape_dc")==13)
    elif monster_name=="Bugbear Stalker":
        source_rider=("reach 10 ft." in text and "{@damage 2d8 + 3}" in text and
            "piercing damage" in text.casefold())
        runtime_rider=(compiled_action.get("attack_modes")==["m"] and
            compiled_action.get("reach_ft")==10 and compiled_action.get("damage")==damage and
            compiled_action.get("damage_type")==damage_type)
    elif monster_name=="Mimic":
        source_rider=("reach 5 ft." in text and "{@damage 1d8 + 3}" in text and
            "{@damage 2d8 + 3}" in text and "{@damage 1d8}" in text and
            "acid damage" in text.casefold())
        runtime_rider=(compiled_action.get("attack_modes")==["m"] and
            compiled_action.get("reach_ft")==5 and compiled_action.get("damage")==damage and
            compiled_action.get("damage_type")==damage_type and
            compiled_action.get("extra_damage_if_target_grappled_by_attacker")=="1d8" and
            compiled_action.get("extra_damage_if_target_grappled_by_attacker_type")=="piercing")
    else:
        source_rider=("reach 10 ft." in text and "range 20/60 ft." in text and
            "{@damage 3d4 + 2}" in text and "bludgeoning damage" in text.casefold())
        runtime_rider=(compiled_action.get("attack_modes")==["m","r"] and
            compiled_action.get("reach_ft")==10 and compiled_action.get("range_normal_ft")==20 and
            compiled_action.get("range_long_ft")==60 and compiled_action.get("damage")==damage and
            compiled_action.get("damage_type")==damage_type)
    return (attack and conditional and source_rider and runtime_rider and
        compiled_action.get("to_hit")==to_hit and compiled_action.get("damage")==damage and
        compiled_action.get("damage_type")==damage_type and
        compiled_action.get("attack_advantage_condition")=="target_grappled_by_attacker" and
        not compiled_action.get("manual_rider_resolution_required",False) and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def check_grappled_target_advantage_attacks(source,compiled):
    by_id={row["id"]:row for row in compiled};errors=[];checked=0
    for (monster,name),_ in GRAPPLED_TARGET_ADVANTAGE_ATTACKS.items():
        src=next((x for x in source if x.get("name")==monster),None);row=by_id.get(monster,{})
        action=next((x for x in (src or {}).get("action",[]) if RCH.sub("",x.get("name","" )).strip()==name),None)
        runtime=next((x for x in row.get("actions",[]) if x.get("name")==name),None)
        if action is None or runtime is None or not grappled_target_advantage_matches(monster,action,runtime):
            errors.append(f"{monster}:{name}:GRAPPLED_TARGET_ADVANTAGE_MISMATCH")
        else:checked+=1
    if checked!=len(GRAPPLED_TARGET_ADVANTAGE_ATTACKS):
        errors.append("GRAPPLED_TARGET_ADVANTAGE_COUNT:"+str(checked))
    return errors,checked

def source_action_requires_manual_resolution(action,monster_name=""):
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
    next_source_turn_end=(RCH.sub("",action.get("name","")).strip()=="Fearsome Claw" and
        "until the end of the scarecrow's next turn" in text.casefold() and
        "frightened" in conditions)
    merrow_next_turn_end=(RCH.sub("",action.get("name","")).strip()=="Bite" and
        "until the end of the merrow's next turn" in text.casefold() and
        "poisoned" in conditions)
    bone_naga_source_turn_start=((monster_name,RCH.sub("",action.get("name","")).strip())==
        ("Bone Naga","Serpentine Gaze") and
        "until the start of the naga's next turn" in text.casefold() and conditions=={"charmed"})
    simple_target_turn_end={
        ("Giant Vulture","Gouge"),("Gas Spore Fungus","Tendril"),
        ("Cloud Giant","Thundercloud"),("Hill Giant","Trash Lob"),
        ("Ghast Gravecaller","Horrific Necrosis")}
    next_target_turn_end=((monster_name,RCH.sub("",action.get("name","" )).strip()) in simple_target_turn_end and
        "until the end of its next turn" in text.casefold() and len(conditions)==1)
    simple_source_turn_start=(monster_name,RCH.sub("",action.get("name","")).strip()) in SOURCE_TURN_START_HIT_CONDITIONS
    simple_source_turn_end=(monster_name,RCH.sub("",action.get("name","")).strip()) in SOURCE_TURN_END_HIT_CONDITIONS
    secondary_attack_damage=(monster_name,RCH.sub("",action.get("name","")).strip()) in SOURCE_SECONDARY_ATTACK_DAMAGE
    chain_devil_typed=(monster_name,RCH.sub("",action.get("name","")).strip())==("Chain Devil","Conjure Infernal Chain")
    advantage_bonus_typed=(monster_name,RCH.sub("",action.get("name","")).strip()) in ADVANTAGE_BONUS_ATTACK_DAMAGE
    wounded_target_advantage_typed=(monster_name,RCH.sub("",action.get("name","")).strip()) in WOUNDED_TARGET_ADVANTAGE_ATTACKS
    grappled_target_advantage_typed=(monster_name,RCH.sub("",action.get("name","" )).strip()) in GRAPPLED_TARGET_ADVANTAGE_ATTACKS
    return (bool(UNMODELED_ACTION_EFFECTS.search(text)) and not chain_devil_typed and not advantage_bonus_typed and not wounded_target_advantage_typed and not grappled_target_advantage_typed) or len(conditions)>1 or recurring or next_source_turn_end or merrow_next_turn_end or bone_naga_source_turn_start or next_target_turn_end or simple_source_turn_start or simple_source_turn_end or secondary_attack_damage

def chain_devil_infernal_chain_matches(action,compiled_action):
    text=json.dumps(action.get("entries",[]),ensure_ascii=False).casefold()
    conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
    return ("{@actsave dex}" in text and "{@dc 15}" in text and
        "one creature the devil can see within 60 feet" in text and
        "{@actsavefail}" in text and "{@damage 2d4 + 4}" in text and
        "fire damage" in text and conditions=={"restrained"} and
        "until the end of the devil's next turn" in text and
        "the chain disappears" in text and "large or smaller" in text and
        "moves the target up to 30 feet straight toward itself" in text and
        compiled_action.get("save_ability")=="dex" and compiled_action.get("save_dc")==15 and
        compiled_action.get("damage")=="2d4 + 4" and compiled_action.get("damage_type")=="fire" and
        compiled_action.get("target_range_ft")==60 and
        compiled_action.get("condition_on_fail")=="restrained" and
        compiled_action.get("condition_duration_rounds")==1 and
        compiled_action.get("condition_duration_tick")=="source_turn_end" and
        compiled_action.get("effect_on_fail")=="pull:30ft;restrained" and
        compiled_action.get("effect_on_fail_movement_max_target_size")=="Large" and
        not compiled_action.get("manual_rider_resolution_required",False) and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def check_chain_devil_infernal_chain(source,compiled):
    monsters={m.get("name"):m for m in source};blocks={m.get("id"):m for m in compiled}
    src=monsters.get("Chain Devil",{});dst=blocks.get("Chain Devil",{})
    action=next((a for a in src.get("action",[]) if RCH.sub("",a.get("name","" )).strip()=="Conjure Infernal Chain"),{})
    compiled_action=next((a for a in dst.get("actions",[]) if a.get("name")=="Conjure Infernal Chain"),{})
    if not action or not compiled_action or not chain_devil_infernal_chain_matches(action,compiled_action):
        return ["Chain Devil:Conjure Infernal Chain:SOURCE_RUNTIME_CONTRACT_MISMATCH"]
    return []

def source_turn_start_hit_condition_matches(monster_name,action,compiled_action):
    name=RCH.sub("",action.get("name","")).strip()
    contract=SOURCE_TURN_START_HIT_CONDITIONS.get((monster_name,name))
    if contract is None:return False
    condition,until=contract
    text=json.dumps(action.get("entries",[]),ensure_ascii=False).casefold()
    source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
    compiled_conditions={x.strip().casefold() for x in compiled_action.get("condition_on_hit","").split(",")}
    suppresses_healing=(monster_name,name) in SOURCE_TURN_START_HEALING_SUPPRESSION
    healing_clause="can't regain" in text.casefold() and "hit points" in text.casefold()
    return (source_conditions=={condition} and compiled_conditions=={condition} and
        until in text and not compiled_action.get("save_dc") and
        not compiled_action.get("save_ability") and not compiled_action.get("condition_on_fail") and
        not compiled_action.get("effect_on_fail") and
        compiled_action.get("condition_duration_rounds")==1 and
        compiled_action.get("condition_duration_tick")=="source_turn_start" and
        (compiled_action.get("prevents_hit_point_regain_while_condition")=="poisoned" and healing_clause
            if suppresses_healing else not compiled_action.get("prevents_hit_point_regain_while_condition")) and
        not compiled_action.get("manual_rider_resolution_required",False) and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def source_turn_end_hit_condition_matches(monster_name,action,compiled_action):
    name=RCH.sub("",action.get("name","")).strip()
    contract=SOURCE_TURN_END_HIT_CONDITIONS.get((monster_name,name))
    if contract is None:return False
    condition,until=contract
    text=json.dumps(action.get("entries",[]),ensure_ascii=False).casefold()
    source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
    compiled_conditions={x.strip().casefold() for x in compiled_action.get("condition_on_hit","").split(",")}
    return (source_conditions=={condition} and compiled_conditions=={condition} and
        until in text and not compiled_action.get("save_dc") and
        not compiled_action.get("save_ability") and not compiled_action.get("condition_on_fail") and
        compiled_action.get("condition_duration_rounds")==1 and
        compiled_action.get("condition_duration_tick")=="source_turn_end" and
        not compiled_action.get("manual_rider_resolution_required",False) and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

def check_source_turn_start_hit_conditions(source,compiled):
    by_source={m.get("name"):m for m in source};by_compiled={m.get("id"):m for m in compiled}
    errors=[];checked=0
    for (monster,name),_ in SOURCE_TURN_START_HIT_CONDITIONS.items():
        source_monster=by_source.get(monster);compiled_monster=by_compiled.get(monster)
        if source_monster is None or compiled_monster is None:
            errors.append(monster+":"+name+":TURN_START_MONSTER_MISSING");continue
        source_action=next((a for a in source_monster.get("action",[]) if RCH.sub("",a.get("name","")).strip()==name),None)
        compiled_action=next((a for a in compiled_monster.get("actions",[]) if a.get("name")==name),None)
        if source_action is None or compiled_action is None:
            errors.append(monster+":"+name+":TURN_START_ACTION_MISSING");continue
        if not source_turn_start_hit_condition_matches(monster,source_action,compiled_action):
            errors.append(monster+":"+name+":TURN_START_CONDITION_CONTRACT_MISMATCH");continue
        checked+=1
    if checked!=len(SOURCE_TURN_START_HIT_CONDITIONS):
        errors.append("TURN_START_CONDITION_RECORD_COUNT:"+str(checked))
    return errors,checked

def source_conditions_in_text(text):
    return {x.strip().casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}

def source_action_has_typed_condition_contract(monster_name,action,compiled_action):
    """Narrowly typed source conditions with an exact, executable expiry boundary."""
    def flatten(value):
        if isinstance(value,str): return value
        if isinstance(value,list): return " ".join(flatten(x) for x in value)
        if isinstance(value,dict): return " ".join(flatten(x) for x in value.values())
        return ""
    text=flatten(action.get("entries",[]))
    name=RCH.sub("",action.get("name","")).strip()
    source_text=text.casefold()
    if (monster_name,name)==("Scout Captain","Aim"):
        return ("{@variantrule advantage|xphb} on the next attack roll it makes during the current turn" in source_text and
            compiled_action.get("category")=="bonus_action" and
            compiled_action.get("grants_advantage_next_attack_this_turn") is True and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    speed_effects={
        ("Warrior Commander","Longbow"):(9,["r"],0,150,600,["3d8 + 5"],"3d8 + 5","piercing",10,"target_turn_end","the target's {@variantrule speed|xphb} decreases by 10 feet until the end of the target's next turn",False),
        ("Berserker Commander","Javelin"):(7,["m","r"],5,30,120,["4d6 + 4"],"4d6 + 4","piercing",5,"source_turn_start","the target's {@variantrule speed|xphb} decreases by 5 feet until the start of the berserker's next turn",False),
        ("Merfolk Skirmisher","Ocean Spear"):(2,["m","r"],5,20,60,["1d6","1d4"],"1d6 + 1d4","piercing + cold",10,"target_turn_end","its {@variantrule speed|xphb} decreases by 10 feet until the end of its next turn",True),
        ("Hobgoblin Warlord","Javelin"):(6,["m","r"],5,30,120,["2d6 + 4"],"2d6 + 4","piercing",10,"source_turn_start","the target's {@variantrule speed|xphb} decreases by 10 feet until the start of the hobgoblin's next turn",False),
        ("Frost Giant","Great Bow"):(9,["r"],0,150,600,["2d10 + 6","2d6"],"2d10 + 6 + 2d6","piercing + cold",10,"target_turn_end","the target's {@variantrule speed|xphb} decreases by 10 feet until the end of its next turn",False),
    }
    if (monster_name,name) in speed_effects:
        hit,modes,reach,normal,long_range,source_dice,damage,damage_type,penalty,tick,phrase,returns=speed_effects[(monster_name,name)]
        hit_text="{@hit "+str(hit)+"}"
        movement=("range "+str(normal)+"/"+str(long_range)+" ft." in source_text)
        if reach: movement=movement and ("reach "+str(reach)+" ft." in source_text)
        return ("{@atkr "+",".join(modes)+"}" in text and hit_text in text and movement and
            [x.strip() for x in re.findall(r"\{@damage\s+([^}]+)\}",text,re.I)]==source_dice and
            phrase in source_text and compiled_action.get("attack_modes")==modes and
            compiled_action.get("to_hit")==hit and compiled_action.get("reach_ft",0)==reach and
            compiled_action.get("range_normal_ft")==normal and compiled_action.get("range_long_ft")==long_range and
            compiled_action.get("damage")==damage and compiled_action.get("damage_type")==damage_type and
            compiled_action.get("speed_reduction_ft")==penalty and
            compiled_action.get("speed_reduction_duration_tick")==tick and
            compiled_action.get("returns_to_attacker_after_ranged_attack",False)==returns and
            (not returns or "the spear magically returns to the merfolk's hand immediately after a ranged attack" in source_text) and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Lacedon Ghoul","Icy Bite"):
        return ("{@atkr m}" in text and "{@hit 4}" in text and
            "reach 5 ft." in source_text and "{@damage 2d6 + 2}" in text and
            "cold damage" in source_text and
            "the target's {@variantrule speed|xphb} decreases by 5 feet until the start of the ghoul's next turn" in source_text and
            compiled_action.get("to_hit")==4 and compiled_action.get("reach_ft")==5 and
            compiled_action.get("damage")=="2d6 + 2" and compiled_action.get("damage_type")=="cold" and
            compiled_action.get("speed_reduction_ft")==5 and
            compiled_action.get("speed_reduction_duration_tick")=="source_turn_start" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Scarecrow","Terrifying Glare"):
        return (source_conditions_in_text(text)=={"frightened","paralyzed"} and
            "{@actsave wis}" in source_text and "{@dc 11}" in source_text and
            "one creature the scarecrow can see within 30 feet" in source_text and
            "until the end of the scarecrow's next turn" in source_text and
            "while {@condition frightened|xphb}, the target has the {@condition paralyzed|xphb} condition" in source_text and
            compiled_action.get("save_ability")=="wis" and compiled_action.get("save_dc")==11 and
            compiled_action.get("target_range_ft")==30 and
            compiled_action.get("condition_on_fail")=="frightened, paralyzed" and
            compiled_action.get("condition_duration_rounds")==1 and
            compiled_action.get("condition_duration_tick")=="source_turn_end" and
            not compiled_action.get("damage") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Doppelganger","Slam"):
        return ("{@variantrule advantage|xphb} during the first round of each combat" in source_text and
            "{@atkr m}" in source_text and "{@hit 6}" in source_text and
            "reach 5 ft." in source_text and "{@damage 2d6 + 4}" in source_text and
            "bludgeoning damage" in source_text and
            compiled_action.get("attack_modes")==["m"] and compiled_action.get("to_hit")==6 and
            compiled_action.get("reach_ft")==5 and compiled_action.get("damage")=="2d6 + 4" and
            compiled_action.get("damage_type")=="bludgeoning" and
            compiled_action.get("attack_advantage_during_first_combat_round") is True and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Yuan-ti Abomination","Poison Spray"):
        return ("{@actsave con}" in source_text and "{@dc 14}" in source_text and
            "each creature in a 30-foot" in source_text and "{@damage 6d6}" in source_text and
            "poison damage" in source_text and "{@actsavesuccess} half damage only." in source_text and
            source_conditions_in_text(text)=={"poisoned","blinded"} and
            "until the end of the yuan-ti's next turn" in source_text and
            compiled_action.get("save_ability")=="con" and compiled_action.get("save_dc")==14 and
            compiled_action.get("target_range_ft")==30 and compiled_action.get("target_area_shape")=="cone" and
            compiled_action.get("damage")=="6d6" and compiled_action.get("damage_type")=="poison" and
            compiled_action.get("damage_on_save")=="6d6" and
            compiled_action.get("damage_on_save_multiplier")==0.5 and
            compiled_action.get("condition_on_fail")=="poisoned, blinded" and
            compiled_action.get("condition_duration_rounds")==1 and
            compiled_action.get("condition_duration_tick")=="source_turn_end" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Tarrasque","Thunderous Bellow"):
        return ("{@actsave con}" in source_text and "{@dc 27}" in source_text and
            "each creature and each object that isn't being worn or carried in a 150-foot" in source_text and
            "{@damage 12d12}" in source_text and "thunder damage" in source_text and
            "{@actsavesuccess} half damage only." in source_text and
            source_conditions_in_text(text)=={"deafened","frightened"} and
            compiled_action.get("save_ability")=="con" and compiled_action.get("save_dc")==27 and
            compiled_action.get("target_range_ft")==150 and compiled_action.get("target_area_shape")=="cone" and
            compiled_action.get("damage")=="12d12" and compiled_action.get("damage_type")=="thunder" and
            compiled_action.get("damage_on_save")=="12d12" and
            compiled_action.get("damage_on_save_multiplier")==0.5 and
            compiled_action.get("condition_on_fail")=="deafened, frightened" and
            compiled_action.get("condition_duration_rounds")==1 and
            compiled_action.get("condition_duration_tick")=="target_turn_end" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name) in SOURCE_CONDITION_RECOVERY_ACTIONS:
        expected,ability,dc,target_range,auto_after,linked_conditions=(
            SOURCE_CONDITION_RECOVERY_ACTIONS[(monster_name,name)])
        target_phrase={
            ("Myconid Sovereign","Pacifying Spores"):
                "one creature the myconid can see within 10 feet",
            ("Myconid Adult","Pacifying Spores (1/Day)"):
                "one creature the myconid can see within 10 feet",
            ("Quasit","Scare (1/Day)"):"one creature within 20 feet",
            ("Carrion Crawler","Paralyzing Tentacles"):
                "one creature the carrion crawler can see within 10 feet",
            ("Doppelganger","Unsettling Visage"):
                "each creature in a 15-foot",
            ("Modron Pentadrone","Paralysis Gas"):
                "each creature in a 30-foot",
            ("Graveyard Revenant","Haunting Glare"):
                "each creature in a 30-foot",
        }[(monster_name,name)]
        source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
        compiled_conditions={x.strip().casefold() for x in
            compiled_action.get("condition_on_fail","").split(",")}
        compiled_linked={str(x).strip().casefold() for x in
            compiled_action.get("condition_recovery_linked_conditions",[])}
        return (source_conditions=={expected,*linked_conditions} and
            compiled_conditions=={expected,*linked_conditions} and
            compiled_action.get("condition_recovery_condition",expected)==expected and
            compiled_linked==set(linked_conditions) and
            target_phrase in source_text and
            ("repeats the save at the end of each of its turns" in source_text or
             "at the end of each of its turns, the target repeats the save" in source_text) and
            "after 1 minute, it succeeds automatically" in source_text and
            ((monster_name,name)==("Modron Pentadrone","Paralysis Gas") and
             "constitution saving throws:" in source_text or
             "{@actsave "+ability+"}" in source_text) and
            re.search(r"\{@dc\s+"+str(dc)+r"\}",source_text) is not None and
            compiled_action.get("save_ability")==ability and compiled_action.get("save_dc")==dc and
            compiled_action.get("target_range_ft")==target_range and
            ((monster_name,name) not in {("Doppelganger","Unsettling Visage"),
                ("Modron Pentadrone","Paralysis Gas"),
                ("Graveyard Revenant","Haunting Glare")} or
             compiled_action.get("target_area_shape")==
                ("cone" if monster_name=="Modron Pentadrone" else "emanation")) and
            compiled_action.get("condition_recovery_save_ability")==ability and
            compiled_action.get("condition_recovery_save_dc")==dc and
            compiled_action.get("condition_recovery_trigger")=="target_turn_end" and
            compiled_action.get("condition_recovery_auto_success_after_failed_saves")==auto_after and
            not compiled_action.get("damage") and
            not compiled_action.get("condition_duration_rounds") and
            not compiled_action.get("condition_duration_tick") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name) in SOURCE_TURN_START_SAVE_CONDITIONS:
        expected,ability,dc,damage,damage_type,target_range,source_until=(
            SOURCE_TURN_START_SAVE_CONDITIONS[(monster_name,name)])
        source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
        compiled_conditions={x.strip().casefold() for x in
            compiled_action.get("condition_on_fail","").split(",")}
        return (source_conditions=={expected} and compiled_conditions=={expected} and
            source_until in source_text and "{@actsave "+ability+"}" in source_text and
            re.search(r"\{@dc\s+"+str(dc)+r"\}",source_text) is not None and
            compiled_action.get("save_ability")==ability and compiled_action.get("save_dc")==dc and
            compiled_action.get("damage","")==damage and
            compiled_action.get("damage_type","")==damage_type and
            compiled_action.get("target_range_ft")==target_range and
            ((monster_name,name) not in {("Doppelganger","Unsettling Visage"),
                ("Modron Pentadrone","Paralysis Gas"),
                ("Graveyard Revenant","Haunting Glare")} or
             compiled_action.get("target_area_shape")==
                ("cone" if monster_name=="Modron Pentadrone" else "emanation")) and
            compiled_action.get("condition_duration_rounds")==1 and
            compiled_action.get("condition_duration_tick")=="source_turn_start" and
            not compiled_action.get("condition_on_hit") and
            not compiled_action.get("effect_on_fail") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name) in GRAPPLED_TARGET_ADVANTAGE_ATTACKS:
        return grappled_target_advantage_matches(monster_name,action,compiled_action)
    if (monster_name,name) in GRAPPLE_ESCAPE_DISADVANTAGE_ACTIONS:
        source_text=text.casefold()
        return ("{@atkr m}" in text and "{@hit 5}" in text and
            "reach 5 ft." in source_text and "{@damage 1d8 + 3}" in text and
            "{@damage 1d8}" in text and "large or smaller creature" in source_text and
            "{@dc 13}" in text and "{@condition grappled|xphb}" in text.casefold() and
            "ability checks made to escape this grapple have {@variantrule disadvantage|xphb}" in text.casefold() and
            compiled_action.get("to_hit")==5 and compiled_action.get("attack_modes")==["m"] and
            compiled_action.get("reach_ft")==5 and compiled_action.get("damage")=="1d8 + 3 + 1d8" and
            compiled_action.get("damage_type")=="bludgeoning + acid" and
            compiled_action.get("escape_dc")==13 and compiled_action.get("condition_on_hit")=="grappled" and
            compiled_action.get("condition_max_target_size")=="large" and
            compiled_action.get("escape_check_disadvantage") is True and
            not compiled_action.get("condition_duration_rounds") and
            not compiled_action.get("condition_duration_tick") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name) in GRAPPLE_LINKED_RESTRAINT_ACTIONS:
        source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
        source_text=text.casefold()
        save_linked=(monster_name,name) in XMM_SAVE_LINKED_RESTRAINT_ACTIONS
        condition_field="condition_on_fail" if save_linked else "condition_on_hit"
        capacity=XMM_GRAPPLE_LINKED_CAPACITY.get((monster_name,name))
        capacity_phrase={
            ("Aberrant Cultist","Tentacle Lash"):"from one of two tentacles",
            ("Chain Devil","Chain"):"from one of two chains",
            ("Giant Octopus","Tentacles"):"from all eight tentacles",
            ("Mezzoloth","Claws"):"from two of four claws",
            ("Kraken","Tentacle"):"from one of ten tentacles"}.get((monster_name,name),"")
        source_save=re.search(r"\{@actSave str\}\s*\{@dc\s*(\d+)\}",text,re.I)
        exact_save=(not save_linked or
            (source_save is not None and compiled_action.get("save_ability")=="str" and
             compiled_action.get("save_dc")==int(source_save.group(1))))
        tail_exclusion=XMM_GRAPPLE_TAIL_EXCLUSIONS.get((monster_name,name),"")
        tail_compiled=compiled_action.get("grappled_target_prevents_action","")
        tail_valid=(not tail_exclusion and not tail_compiled) or (
            tail_exclusion in source_text and tail_compiled=="Tail")
        return (source_conditions=={"grappled","restrained"} and exact_save and
            "escape {@dc "+str(compiled_action.get("escape_dc",0))+"}" in text and
            ("until the grapple ends" in source_text or
             "while {@condition grappled|xphb}, the target has the {@condition restrained|xphb} condition" in source_text) and
            compiled_action.get(condition_field)=="grappled, restrained" and
            compiled_action.get("linked_condition_bundle_until_grapple_ends") is True and
            compiled_action.get("condition_max_target_size")==
                XMM_GRAPPLE_LINKED_CONTRACT_SIZES.get((monster_name,name),"") and
            compiled_action.get("escape_dc")==XMM_GRAPPLE_LINKED_CONTRACT_DCS.get((monster_name,name)) and
            (compiled_action.get("grapple_target_capacity")==capacity if capacity is not None
                else not compiled_action.get("grapple_target_capacity",0)) and
            (not capacity_phrase or capacity_phrase in source_text) and tail_valid and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name) in ADVANTAGE_BONUS_ATTACK_DAMAGE:
        return advantage_bonus_attack_matches(monster_name,action,compiled_action)
    if (monster_name,name) in WOUNDED_TARGET_ADVANTAGE_ATTACKS:
        return wounded_target_advantage_matches(monster_name,action,compiled_action)
    if (monster_name,name)==("Merrow","Harpoon"):
        return ("{@atkr m,r}" in text and "{@hit 6}" in text and
            "range 20/60 ft." in source_text and
            "{@damage 2d6 + 4}" in text and
            "large or smaller creature" in source_text and
            "pulls the target up to 15 feet straight toward itself" in source_text and
            compiled_action.get("effect_on_hit")=="pull:15ft" and
            compiled_action.get("effect_on_hit_max_target_size")=="Large" and
            compiled_action.get("damage")=="2d6 + 4" and
            compiled_action.get("damage_type")=="piercing" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Hook Horror","Hook"):
        return ("{@atkr m}" in text and "{@hit 6}" in text and
            "reach 10 ft." in source_text and "{@damage 2d6 + 4}" in text and
            "large or smaller creature" in source_text and
            "moves the target 5 feet straight toward or away from itself" in source_text and
            compiled_action.get("effect_on_hit")=="either:5ft" and
            compiled_action.get("effect_on_hit_max_target_size")=="Large" and
            compiled_action.get("damage")=="2d6 + 4" and
            compiled_action.get("damage_type")=="piercing" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Satyr","Hooves"):
        return ("{@atkr m}" in text and "{@hit 5}" in text and
            "reach 5 ft." in source_text and "{@damage 1d4 + 3}" in text and
            "medium or smaller creature" in source_text and
            "pushes the target up to 10 feet straight away from itself" in source_text and
            compiled_action.get("effect_on_hit")=="push:10ft" and
            compiled_action.get("effect_on_hit_max_target_size")=="Medium" and
            compiled_action.get("damage")=="1d4 + 3" and
            compiled_action.get("damage_type")=="bludgeoning" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Balor" and name=="Flame Whip":
        return ("{@atkr m}" in text and "{@hit 14}" in text and
            "reach 30 ft." in source_text and "{@damage 3d6 + 8}" in text and
            "{@damage 5d6}" in text and "force damage" in source_text and
            "fire damage" in source_text and "huge or smaller creature" in source_text and
            "pulls the target up to 25 feet straight toward itself" in source_text and
            "{@condition prone|xphb}" in text.casefold() and
            compiled_action.get("effect_on_hit")=="pull:25ft;prone" and
            compiled_action.get("effect_on_hit_max_target_size")=="Huge" and
            compiled_action.get("condition_on_hit")=="prone" and
            compiled_action.get("condition_ends_when")=="stands_up" and
            compiled_action.get("condition_max_target_size")=="Huge" and
            compiled_action.get("damage")=="3d6 + 8 + 5d6" and
            compiled_action.get("damage_type")=="force + fire" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Shambling Mound","Charged Tendril"):
        return ("{@atkr m}" in text and "{@hit 7}" in text and
            "reach 10 ft." in source_text and "{@damage 1d6 + 4}" in text and
            "{@damage 2d4}" in text and "bludgeoning damage" in source_text and
            "lightning damage" in source_text and "medium or smaller creature" in source_text and
            "pulls the target 5 feet straight toward itself" in source_text and
            compiled_action.get("effect_on_hit")=="pull:5ft" and
            compiled_action.get("effect_on_hit_max_target_size")=="Medium" and
            compiled_action.get("damage")=="1d6 + 4 + 2d4" and
            compiled_action.get("damage_type")=="bludgeoning + lightning" and
            compiled_action.get("reach_ft")==10 and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Colossus" and name=="Slam":
        return ("pushes the target up to 20 feet straight away from itself" in source_text and
            "{@atkr m}" in text and "{@hit 18}" in text and
            "{@damage 4d10 + 10}" in text and
            compiled_action.get("effect_on_hit")=="push:20ft" and
            compiled_action.get("damage")=="4d10 + 10" and
            compiled_action.get("damage_type")=="bludgeoning" and
            compiled_action.get("reach_ft")==20 and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Tough Boss" and name=="Warhammer":
        return ("{@atkr m}" in text and "{@hit 5}" in text and
            "reach 5 ft." in source_text and "{@damage 2d8 + 3}" in text and
            "large or smaller creature" in source_text and
            "pushes the target up to 10 feet straight away from itself" in source_text and
            compiled_action.get("effect_on_hit")=="push:10ft" and
            compiled_action.get("effect_on_hit_max_target_size")=="Large" and
            compiled_action.get("damage")=="2d8 + 3" and
            compiled_action.get("damage_type")=="bludgeoning" and
            compiled_action.get("reach_ft")==5 and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Goristro" and name=="Brutal Gore":
        return ("{@atkr m}" in text and "{@hit 13}" in text and
            "reach 10 ft." in source_text and "{@damage 6d10 + 7}" in text and
            "huge or smaller creature" in source_text and
            "pushed up to 20 feet straight away from the goristro" in source_text and
            "{@condition prone|xphb}" in text.casefold() and
            compiled_action.get("effect_on_hit")=="push:20ft;prone" and
            compiled_action.get("effect_on_hit_max_target_size")=="Huge" and
            compiled_action.get("condition_on_hit")=="prone" and
            compiled_action.get("condition_ends_when")=="stands_up" and
            compiled_action.get("condition_max_target_size")=="Huge" and
            compiled_action.get("damage")=="6d10 + 7" and
            compiled_action.get("damage_type")=="piercing" and
            compiled_action.get("reach_ft")==10 and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Poltergeist" and name=="Telekinetic Thrust":
        return ("{@actSave str}" in text and "{@dc 12}" in text and
            "one creature the poltergeist can see within 30 feet" in source_text and
            "{@actsavefail}" in text.casefold() and "{@damage 2d6 + 2}" in text and
            "force damage" in source_text and
            "the target is pushed up to 30 feet straight away from the poltergeist" in source_text and
            compiled_action.get("save_ability")=="str" and compiled_action.get("save_dc")==12 and
            compiled_action.get("effect_on_fail")=="push:30ft" and
            compiled_action.get("damage")=="2d6 + 2" and
            compiled_action.get("damage_type")=="force" and
            not compiled_action.get("damage_on_save") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    bronze_breaths={
        ("Adult Bronze Dragon","Repulsion Breath"):(19,60),
        ("Ancient Bronze Dragon","Repulsion Breath"):(23,60),
        ("Bronze Dragon Wyrmling","Repulsion Breath"):(12,30),
        ("Young Bronze Dragon","Repulsion Breath"):(15,40),
    }
    bronze=bronze_breaths.get((monster_name,name))
    if bronze:
        dc,distance=bronze
        return ("{@actSave str}" in text and f"{{@dc {dc}}}" in text and
            "30-foot" in source_text and "{@actsavefail}" in text.casefold() and
            f"pushed up to {distance} feet straight away from the dragon" in source_text and
            "{@condition prone|xphb}" in text.casefold() and
            compiled_action.get("save_ability")=="str" and compiled_action.get("save_dc")==dc and
            compiled_action.get("effect_on_fail")==f"push:{distance}ft;prone" and
            compiled_action.get("condition_on_fail")=="prone" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Air Elemental","Whirlwind"):
        return ("{@actSave str}" in text and "{@dc 13}" in text and
            "{@recharge 4}" in action.get("name","") and "medium or smaller creature" in source_text and
            "{@actsavefail}" in text.casefold() and "{@damage 4d10 + 2}" in text and
            "thunder damage" in source_text and "pushed up to 20 feet straight away from the elemental" in source_text and
            "{@condition prone|xphb}" in text.casefold() and "half damage only" in source_text and
            compiled_action.get("save_dc")==13 and compiled_action.get("save_ability")=="str" and
            compiled_action.get("damage")=="4d10 + 2" and compiled_action.get("damage_on_save_multiplier")==0.5 and
            compiled_action.get("effect_on_fail")=="push:20ft;prone" and
            compiled_action.get("effect_on_fail_max_target_size")=="Medium" and
            compiled_action.get("condition_on_fail")=="prone" and
            compiled_action.get("condition_on_fail_max_target_size")=="Medium" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Marid","Water Jet"):
        return ("{@actSave dex}" in text and "{@dc 18}" in text and
            "{@actsavefail}" in text.casefold() and "{@damage 9d6}" in text and
            "cold damage" in source_text and "huge or smaller creature" in source_text and
            "pushed up to 20 feet straight away from the marid" in source_text and
            "{@condition prone|xphb}" in text.casefold() and "half damage only" in source_text and
            compiled_action.get("save_dc")==18 and compiled_action.get("save_ability")=="dex" and
            compiled_action.get("damage")=="9d6" and compiled_action.get("damage_on_save_multiplier")==0.5 and
            compiled_action.get("effect_on_fail")=="push:20ft;prone" and
            compiled_action.get("effect_on_fail_max_target_size")=="Huge" and
            compiled_action.get("condition_on_fail")=="prone" and
            compiled_action.get("condition_on_fail_max_target_size")=="Huge" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name)==("Bone Naga","Serpentine Gaze"):
        return ("{@actSave wis}" in text and "{@dc 13}" in text and
            "one creature the naga can see within 60 feet" in source_text and
            "{@actsavefail}" in text.casefold() and "{@damage 3d6 + 3}" in text and
            "psychic damage" in source_text and "charmed" in source_text and
            "until the start of the naga's next turn" in source_text and
            compiled_action.get("save_ability")=="wis" and compiled_action.get("save_dc")==13 and
            compiled_action.get("damage")=="3d6 + 3" and compiled_action.get("damage_type")=="psychic" and
            compiled_action.get("condition_on_fail")=="charmed" and
            compiled_action.get("condition_duration_rounds")==1 and
            compiled_action.get("condition_duration_tick")=="source_turn_start" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Aarakocra Skirmisher" and name=="Wind Javelin":
        return ("magically returns to the aarakocra's hand immediately after a ranged attack" in text.casefold() and
            "{@atkr m,r}" in text and
            compiled_action.get("returns_to_attacker_after_ranged_attack") is True and
            compiled_action.get("damage")=="1d6 + 2 + 1d4" and
            compiled_action.get("damage_type")=="piercing + thunder" and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    returning_weapons={
        ("Kenku","Shadow Blade"):"the kenku's hand",
        ("Mezzoloth","Mercurial Trident"):"the mezzoloth's claw",
        ("Modron Duodrone","Clockwork Blade"):"the modron's hand",
        ("Modron Tridrone","Clockwork Spear"):"the modron's hand",
        ("Nycaloth","Mercurial Axe"):"the nycaloth's hand",
        ("Salamander","Flame Spear"):"the salamander's hand",
        ("Salamander Inferno Master","Flame Trident"):"the salamander's hand",
    }
    return_to=returning_weapons.get((monster_name,name))
    if return_to:
        return ("{@atkr m,r}" in text and
            "magically returns to "+return_to in source_text and
            "immediately after a ranged attack" in source_text and
            compiled_action.get("returns_to_attacker_after_ranged_attack") is True and
            not compiled_action.get("condition_on_hit") and
            not compiled_action.get("effect_on_hit") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Bearded Devil" and name=="Beard":
        source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
        compiled_conditions={x.strip().casefold() for x in compiled_action.get("condition_on_hit","").split(",")}
        source_text=text.casefold()
        return (source_conditions=={"poisoned"} and compiled_conditions=={"poisoned"} and
            "until the start of the devil's next turn" in source_text and
            "can't regain" in source_text and "hit points" in source_text and
            compiled_action.get("prevents_hit_point_regain_while_condition")=="poisoned" and
            compiled_action.get("condition_duration_rounds")==1 and
            compiled_action.get("condition_duration_tick")=="source_turn_start" and
            not compiled_action.get("save_dc") and not compiled_action.get("save_ability") and
            not compiled_action.get("condition_on_fail") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name=="Death Cultist" and name=="Dread Scythe":
        source_text=text.casefold()
        return ("can't regain" in source_text and "until the end of its next turn" in source_text and
            not re.search(r"\{@condition |\{@actSave",text,re.I) and
            compiled_action.get("healing_suppression_duration_rounds")==1 and
            compiled_action.get("healing_suppression_tick")=="target_turn_end" and
            not compiled_action.get("condition_on_hit") and not compiled_action.get("condition_on_fail") and
            not compiled_action.get("manual_rider_resolution_required",False) and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if (monster_name,name) in SOURCE_TURN_START_HIT_CONDITIONS:
        return source_turn_start_hit_condition_matches(monster_name,action,compiled_action)
    if (monster_name,name) in SOURCE_TURN_END_HIT_CONDITIONS:
        return source_turn_end_hit_condition_matches(monster_name,action,compiled_action)
    if monster_name=="Rakshasa" and name=="Baleful Command":
        expected={"frightened","incapacitated"}; field="condition_on_fail"
        source_until="until the start of the rakshasa's next turn"
    elif monster_name=="Storm Giant" and name=="Thunderbolt":
        expected={"blinded","deafened"}; field="condition_on_hit"
        source_until="until the start of the giant's next turn"
    elif monster_name=="Scarecrow" and name=="Fearsome Claw":
        expected={"frightened"}; field="condition_on_hit"
        source_until="until the end of the scarecrow's next turn"
    elif monster_name=="Merrow" and name=="Bite":
        expected={"poisoned"}; field="condition_on_hit"
        source_until="until the end of the merrow's next turn"
    elif monster_name=="Giant Vulture" and name=="Gouge":
        expected={"poisoned"}; field="condition_on_hit"
        source_until="until the end of its next turn"
    elif monster_name=="Gas Spore Fungus" and name=="Tendril":
        expected={"poisoned"}; field="condition_on_hit"
        source_until="until the end of its next turn"
    elif monster_name=="Cloud Giant" and name=="Thundercloud":
        expected={"incapacitated"}; field="condition_on_hit"
        source_until="until the end of its next turn"
    elif monster_name=="Hill Giant" and name=="Trash Lob":
        expected={"poisoned"}; field="condition_on_hit"
        source_until="until the end of its next turn"
    elif monster_name=="Ghast Gravecaller" and name=="Horrific Necrosis":
        expected={"frightened"}; field="condition_on_hit"
        source_until="until the end of its next turn"
    else:return False
    if field=="condition_on_hit" and source_until.startswith("until the end"):
        source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
        compiled_conditions={x.strip().casefold() for x in compiled_action.get(field,"").split(",")}
        return (source_conditions==expected and compiled_conditions==expected and
            source_until in text.casefold() and not compiled_action.get("save_dc") and
            not compiled_action.get("save_ability") and not compiled_action.get("condition_on_fail") and
            compiled_action.get("condition_duration_rounds")==1 and
            compiled_action.get("condition_duration_tick")==(
                "target_turn_end" if (monster_name,name) in TARGET_TURN_END_HIT_CONDITIONS
                else "source_turn_end") and
            not compiled_action.get("manual_unmodeled_source_effect_required",False))
    if monster_name not in ("Rakshasa","Storm Giant"):return False
    source_conditions={x.casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
    compiled_conditions={x.strip().casefold() for x in compiled_action.get(field,"").split(",")}
    return (source_conditions==expected and compiled_conditions==expected and
        source_until in text.casefold() and
        not re.search(r"\brepeats? the save\b|\bat the end of each of its turns\b|\bchoice\b",text,re.I) and
        compiled_action.get("condition_duration_rounds")==1 and
        compiled_action.get("condition_duration_tick")=="source_turn_start" and
        not compiled_action.get("manual_unmodeled_source_effect_required",False))

CONDITION_CHOICE_CONTRACTS={
    ("Death Slaad","Chaos Blade"):("charmed, frightened, poisoned, incapacitated","d4",1,"source_turn_start","",("rolling {@dice 1d4}","on a 1","on a 2","on a 3","on a 4")),
    ("Gray Slaad","Chaos Claw"):("charmed, frightened, poisoned, incapacitated","d4",1,"source_turn_start","",("rolling {@dice 1d4}","on a 1","on a 2","on a 3","on a 4")),
    ("Green Slaad","Chaos Staff"):("charmed, frightened, poisoned, incapacitated","d4",1,"source_turn_start","",("rolling {@dice 1d4}","on a 1","on a 2","on a 3","on a 4")),
    ("Githzerai Psion","Psychic Warp"):("charmed, prone","player_choice",0,"","large",("githzerai's choice","(a)","(b)","large or smaller")),
    ("Pixie","Faerie Dust"):("charmed, poisoned","player_choice",1,"source_turn_start","",("pixie's choice",)),
    ("Pixie Wonderbringer","Faerie Dust"):("charmed, poisoned","player_choice",1,"source_turn_start","",("pixie's choice",)),
}

def condition_choice_contract_matches(monster_name,action,compiled_action):
    expected=CONDITION_CHOICE_CONTRACTS.get((monster_name,RCH.sub("",action.get("name","" )).strip()))
    if expected is None:return False
    choices,mode,rounds,tick,max_size,phrases=expected
    def flatten(value):
        if isinstance(value,str):return value
        if isinstance(value,list):return " ".join(flatten(x) for x in value)
        if isinstance(value,dict):return " ".join(flatten(x) for x in value.values())
        return ""
    text=flatten(action.get("entries",[])).casefold()
    source_conditions=[x.strip().casefold() for x in re.findall(r"\{@condition ([^|}]+)",text,re.I)]
    expected_conditions=[x.strip() for x in choices.split(",")]
    return (all(p in text for p in phrases) and
        source_conditions==expected_conditions and
        compiled_action.get("condition_choice_on_hit")==choices and
        compiled_action.get("condition_duration_rounds",0)==rounds and
        compiled_action.get("condition_duration_tick","")==tick and
        compiled_action.get("condition_choice_max_target_size","")==max_size and
        compiled_action.get("manual_rider_resolution_required") is True and
        compiled_action.get("manual_unmodeled_source_effect_required") is True)

def check_condition_choice_contracts(source,compiled):
    by_id={m["id"]:m for m in compiled}; by_source={m.get("name"):m for m in source};errors=[];matched=0
    for key in CONDITION_CHOICE_CONTRACTS:
        monster,action_name=key;src=by_source.get(monster);dst=by_id.get(monster,{})
        if src is None:
            errors.append(f"{monster}:{action_name}:SOURCE_MONSTER_MISSING");continue
        source_actions=src.get("action",[]);slot=next((i for i,a in enumerate(source_actions) if RCH.sub("",a.get("name","" )).strip()==action_name),-1)
        destination=dst.get("actions",[]);compiled_action=destination[slot] if 0<=slot<len(destination) else {}
        if slot<0 or not condition_choice_contract_matches(monster,source_actions[slot],compiled_action):
            errors.append(f"{monster}:{action_name}:CONDITION_CHOICE_SOURCE_MISMATCH")
        else:
            matched+=1
    compiled_choice_keys={(m.get("id"),a.get("name")) for m in compiled for a in m.get("actions",[]) if a.get("condition_choice_on_hit")}
    if compiled_choice_keys!=set(CONDITION_CHOICE_CONTRACTS):
        errors.append("CONDITION_CHOICE_CATALOG_MISMATCH")
    return errors,matched

def check_sphinx_of_secrets_multiattack(source,plans):
    """Verify the one-attack substitution variant against its exact XMM action."""
    monster=next((m for m in source if m.get("name")=="Sphinx of Secrets"),None)
    plan=next((p for p in plans if p.get("id")=="Sphinx of Secrets"),None)
    if monster is None or plan is None:return ["SPHINX_MULTIATTACK_SOURCE_OR_PLAN_MISSING"],0
    action=next((a for a in monster.get("action",[]) if a.get("name")=="Multiattack"),None)
    if action is None:return ["SPHINX_MULTIATTACK_ACTION_MISSING"],0
    text=json.dumps(action.get("entries",[]),ensure_ascii=False).casefold()
    source_ok=("makes three claw attacks" in text and "replace one attack" in text and
        "curses" in text and "of the riddle" in text)
    expected=[
        [{"k":"attack","o":["Claw"],"c":3}],
        [{"k":"attack","o":["Claw"],"c":2},
         {"k":"action","o":["Curse of the Riddle"],"c":1}],
        [{"k":"attack","o":["Claw"],"c":1},
         {"k":"action","o":["Curse of the Riddle"],"c":1},
         {"k":"attack","o":["Claw"],"c":1}],
        [{"k":"action","o":["Curse of the Riddle"],"c":1},
         {"k":"attack","o":["Claw"],"c":2}],
    ]
    pointer_ok=plan.get("ptr")=="/monster/407/action/0/entries"
    valid=source_ok and pointer_ok and plan.get("v")==expected
    return ([] if valid else ["SPHINX_MULTIATTACK_SOURCE_PLAN_MISMATCH"]),int(valid)

WEREBEAST_MULTIATTACKS={
    "Werebear":("Handaxe (Humanoid or Hybrid Form Only)","Rend (Bear or Hybrid Form Only)","Bite (Bear or Hybrid Form Only)","The werebear makes two attacks, using Handaxe or Rend in any combination. It can replace one attack with a Bite attack."),
    "Wereboar":("Javelin (Humanoid or Hybrid Form Only)","Tusk (Boar or Hybrid Form Only)","Gore (Boar or Hybrid Form Only)","The wereboar makes two attacks, using Javelin or Tusk in any combination. It can replace one attack with a Gore attack."),
    "Wererat":("Scratch","Hand Crossbow (Humanoid or Hybrid Form Only)","Bite (Rat or Hybrid Form Only)","The wererat makes two attacks, using Scratch or Hand Crossbow in any combination. It can replace one attack with a Bite attack."),
    "Weretiger":("Scratch","Longbow (Humanoid or Hybrid Form Only)","Bite (Tiger or Hybrid Form Only)","The weretiger makes two attacks, using Scratch or Longbow in any combination. It can replace one attack with a Bite attack."),
    "Werewolf":("Scratch","Longbow (Humanoid or Hybrid Form Only)","Bite (Wolf or Hybrid Form Only)","The werewolf makes two attacks, using Scratch or Longbow in any combination. It can replace one attack with a Bite attack."),
}

def expected_werebeast_multiattack_variants(first,second,substitute):
    attack=lambda options,count:{"k":"attack","o":options,"c":count}
    return [
        [attack([first,second],2)],
        [attack([substitute],1),attack([first,second],1)],
        [attack([first,second],1),attack([substitute],1)],
    ]

def check_werebeast_multiattack_variants(source,plans):
    source_by_name={m.get("name"):m for m in source}
    plan_by_name={p.get("id"):p for p in plans}
    errors=[]
    for monster,(first,second,substitute,expected_text) in WEREBEAST_MULTIATTACKS.items():
        block=source_by_name.get(monster)
        action=next((a for a in (block or {}).get("action",[]) if a.get("name")=="Multiattack"),None)
        plan=plan_by_name.get(monster)
        expected_variants=expected_werebeast_multiattack_variants(first,second,substitute)
        if (action is None or " ".join(action.get("entries",[]))!=expected_text or plan is None or
                plan.get("ptr")!=f"/monster/{source.index(block)}/action/0/entries" or
                plan.get("v")!=expected_variants):
            errors.append(monster+":WEREBEAST_MULTIATTACK_SOURCE_PLAN_MISMATCH")
            continue
        action_names={a.get("name") for a in block.get("action",[]) if a.get("name")!="Multiattack"}
        if not {first,second,substitute}.issubset(action_names):
            errors.append(monster+":WEREBEAST_MULTIATTACK_ACTION_MISSING")
    return errors,0 if errors else len(WEREBEAST_MULTIATTACKS)

COMPOUND_MULTIATTACKS={
    "Barbed Devil":("The devil makes one Claws attack and one Tail attack, or it makes two Hurl Flame attacks.",
        ([[{"k":"attack","o":["Claws"],"c":1},{"k":"attack","o":["Tail"],"c":1}],
          [{"k":"attack","o":["Hurl Flame"],"c":2}]],None)),
    "Clay Golem":("The golem makes two Slam attacks, or it makes three Slam attacks if it used Hasten this turn.",
        ([[{"k":"attack","o":["Slam"],"c":2}],[{"k":"attack","o":["Slam"],"c":3}]],
         {"field":"hasten_used_this_turn","variant_index":1})),
    "Dragon Turtle":("The dragon makes three Bite attacks. It can replace one attack with a Tail attack.",
        ([[{"k":"attack","o":["Bite"],"c":3}],
          [{"k":"attack","o":["Tail"],"c":1},{"k":"attack","o":["Bite"],"c":2}],
          [{"k":"attack","o":["Bite"],"c":1},{"k":"attack","o":["Tail"],"c":1},{"k":"attack","o":["Bite"],"c":1}],
          [{"k":"attack","o":["Bite"],"c":2},{"k":"attack","o":["Tail"],"c":1}]],None)),
    "Medusa":("The medusa makes two Claw attacks and one Snake Hair attack, or it makes three Poison Ray attacks.",
        ([[{"k":"attack","o":["Claw"],"c":2},{"k":"attack","o":["Snake Hair"],"c":1}],
          [{"k":"attack","o":["Poison Ray"],"c":3}]],None)),
    "Roc":("The roc makes two Beak attacks. It can replace one attack with a Talons attack.",
        ([[{"k":"attack","o":["Beak"],"c":2}],
          [{"k":"attack","o":["Talons"],"c":1},{"k":"attack","o":["Beak"],"c":1}],
          [{"k":"attack","o":["Beak"],"c":1},{"k":"attack","o":["Talons"],"c":1}]],None)),
    "Tarrasque":("The tarrasque makes one Bite attack and three other attacks, using Claw or Tail in any combination.",
        ([[{"k":"attack","o":["Bite"],"c":1},{"k":"attack","o":["Claw","Tail"],"c":3}]],None)),
}
def expected_single_attack_replacement(attack,substitute):
    variants=[[{"k":"attack","o":[attack],"c":3}]]
    for slot in range(3):
        steps=[]
        if slot:steps.append({"k":"attack","o":[attack],"c":slot})
        steps.append({"k":"use","o":[substitute],"c":1})
        if slot<2:steps.append({"k":"attack","o":[attack],"c":2-slot})
        variants.append(steps)
    return variants

for _monster,_substitute,_text in (
    ("Young Bronze Dragon","Repulsion Breath","The dragon makes three Rend attacks. It can replace one attack with a use of Repulsion Breath."),
    ("Young Copper Dragon","Slowing Breath","The dragon makes three Rend attacks. It can replace one attack with a use of Slowing Breath."),
    ("Young Gold Dragon","Weakening Breath","The dragon makes three Rend attacks. It can replace one attack with a use of Weakening Breath."),
    ("Young Silver Dragon","Paralyzing Breath","The dragon makes three Rend attacks. It can replace one attack with a use of Paralyzing Breath."),
):
    COMPOUND_MULTIATTACKS[_monster]=(_text,(expected_single_attack_replacement("Rend",_substitute),None))

def check_compound_multiattack_variants(source,plans):
    source_by_name={m.get("name"):m for m in source};plan_by_name={p.get("id"):p for p in plans}
    errors=[]
    for monster,(expected_text,(expected_variants,context)) in COMPOUND_MULTIATTACKS.items():
        block=source_by_name.get(monster)
        action=next((a for a in (block or {}).get("action",[]) if a.get("name")=="Multiattack"),None)
        plan=plan_by_name.get(monster)
        if (action is None or " ".join(action.get("entries",[]))!=expected_text or plan is None or
                plan.get("ptr")!=f"/monster/{source.index(block)}/action/0/entries" or
                plan.get("v")!=expected_variants or plan.get("context")!=context):
            errors.append(monster+":COMPOUND_MULTIATTACK_SOURCE_PLAN_MISMATCH")
            continue
        names={a.get("name") for a in block.get("action",[]) if a.get("name")!="Multiattack"}
        if not {name for group in expected_variants for step in group for name in step["o"]}.issubset(names):
            errors.append(monster+":COMPOUND_MULTIATTACK_ACTION_MISSING")
    return errors,0 if errors else len(COMPOUND_MULTIATTACKS)

def _expected_simple_multiattack_plan(monster,action):
    choice=simple_multiattack_plan(action)
    if choice is not None:
        return [[{"k":"attack","o":choice[1].split("|"),"c":choice[0]}]]
    repeated=simple_repeat_multiattack_plan(monster,action)
    if repeated is not None:
        return [[{"k":"attack","o":[repeated[1]],"c":repeated[0]}]]
    ordered=simple_ordered_multiattack_plan(monster,action)
    if ordered is None:return None
    groups=[]
    for name in ordered:
        if groups and groups[-1]["o"]==[name]:groups[-1]["c"]+=1
        else:groups.append({"k":"attack","o":[name],"c":1})
    return [groups]

def check_multiattack_plan_catalog(source,plans,expected_structured=242,expected_simple=197,
                                   expected_total=306):
    source_rows=[]
    for monster_index,monster in enumerate(source):
        for slot,action in enumerate(monster.get("action",[])):
            if action.get("name")=="Multiattack":
                source_rows.append((monster_index,monster,slot,action))
    plan_by_id={row.get("id"):row for row in plans};errors=[];simple_count=0
    if len(source_rows)!=expected_total or len(plans)!=expected_total or len(plan_by_id)!=expected_total:
        errors.append("MULTIATTACK_CATALOG_SOURCE_COUNT_MISMATCH")
    for monster_index,monster,slot,action in source_rows:
        plan=plan_by_id.get(monster.get("name"),{})
        expected_ptr=f"/monster/{monster_index}/action/{slot}/entries"
        if plan.get("ptr")!=expected_ptr:
            errors.append(monster["name"]+":MULTIATTACK_SOURCE_POINTER_MISMATCH")
        expected=_expected_simple_multiattack_plan(monster,action)
        if expected is not None:
            simple_count+=1
            if plan.get("v")!=expected:
                errors.append(monster["name"]+":SIMPLE_MULTIATTACK_PLAN_SOURCE_MISMATCH")
    structured=sum(bool(row.get("v")) for row in plans)
    if structured!=expected_structured:
        errors.append("MULTIATTACK_STRUCTURED_SOURCE_COUNT:"+str(structured))
    if simple_count!=expected_simple:
        errors.append("SIMPLE_MULTIATTACK_SOURCE_COUNT:"+str(simple_count))
    return errors,structured,simple_count,len(source_rows)-structured

def expected_save_condition_profiles(source,compiled):
    by_id={row.get("id"):row for row in compiled};out=[]
    categories=(("action","actions","action"),("bonus","special_actions","bonus_action"),
        ("reaction","special_actions","reaction"),("legendary","special_actions","legendary_action"))
    for monster_index,monster in enumerate(source):
        block=by_id.get(monster.get("name"),{})
        for source_field,dest_field,category in categories:
            destination=block.get(dest_field,[])
            if category!="action":
                destination=[row for row in destination if row.get("category")==category]
            for slot,action in enumerate(monster.get(source_field,[])):
                compiled_action=destination[slot] if slot<len(destination) else {}
                if (not compiled_action.get("condition_on_fail") or
                    not source_action_requires_manual_resolution(action,monster.get("name","")) or
                    source_action_has_typed_condition_contract(monster.get("name",""),action,compiled_action)):
                    continue
                out.append({
                    "id":monster["name"],
                    "action":RCH.sub("",action.get("name","")).strip(),
                    "category":category,
                    "slot":slot,
                    "source_pointer":f"/monster/{monster_index}/{source_field}/{slot}/entries/0",
                    "conditions":compiled_action["condition_on_fail"],
                    "save_ability":compiled_action.get("save_ability",""),
                    "save_dc":compiled_action.get("save_dc",0),
                    "target_max_size":compiled_action.get("condition_max_target_size",""),
                    "target_range_ft":compiled_action.get("target_range_ft",0),
                    "target_area_shape":compiled_action.get("target_area_shape",""),
                    "additional_source_effects_manual":bool(
                        compiled_action.get("damage") or compiled_action.get("damage_on_save") or
                        compiled_action.get("effect_on_fail") or compiled_action.get("spells")),
                })
    return out

def check_save_condition_profiles(source,compiled,profiles,expected_count=58):
    expected=expected_save_condition_profiles(source,compiled)
    errors=[]
    if len(expected)!=expected_count:errors.append("MANUAL_SAVE_CONDITION_SOURCE_COUNT:"+str(len(expected)))
    if profiles!=expected:errors.append("MANUAL_SAVE_CONDITION_PROFILE_SOURCE_MISMATCH")
    return errors,len(expected) if profiles==expected else 0

RECURRING_DAMAGE_CONTRACTS={
    ("Pit Fiend","Bite"):("/monster/346/action/1/entries/0","6d6","poison",
        "target_turn_start","poisoned",("6d6","start of each of its turns","end of each of its turns","after 1 minute"),"con",21,10),
    ("Stirge","Proboscis"):("/monster/417/action/0/entries/0","2d4","necrotic",
        "source_turn_start","attached",("2d4","start of each of the stirge's turns","detach itself","detach the stirge"),"",0,0),
    ("Vine Blight","Constricting Vine"):("/monster/456/action/0/entries/0","1d8","bludgeoning",
        "target_turn_start","grappled",("1d8","until the grapple ends","escape {@dc 12}","Large or smaller"),"",0,0),
}

def check_recurring_damage_profiles(source,compiled,profiles):
    source_by_name={m.get("name"):m for m in source};compiled_by_name={m.get("id"):m for m in compiled}
    profile_by_name={(p.get("id"),p.get("action")):p for p in profiles};errors=[]
    for (monster,action_name),expected in RECURRING_DAMAGE_CONTRACTS.items():
        pointer,dice,damage_type,tick,active_condition,phrases,recovery_ability,recovery_dc,auto_rounds=expected
        src=source_by_name.get(monster);dst=compiled_by_name.get(monster,{})
        action=next((a for a in (src or {}).get("action",[]) if RCH.sub("",a.get("name","")).strip()==action_name),None)
        compiled_action=next((a for a in dst.get("actions",[]) if a.get("name")==action_name),None)
        profile=profile_by_name.get((monster,action_name))
        if src is None or action is None or compiled_action is None or profile is None:
            errors.append(monster+":"+action_name+":RECURRING_SOURCE_OR_PROFILE_MISSING");continue
        text=json.dumps(action.get("entries",[]),ensure_ascii=False).casefold()
        conditions={c.casefold() for c in re.findall(r"\{@condition ([^|}]+)",text,re.I)}
        if (profile.get("ptr")!=pointer or profile.get("damage")!=dice or
            profile.get("damage_type")!=damage_type or profile.get("tick")!=tick or
            profile.get("active_condition")!=active_condition or
            not compiled_action.get("source_bound") or
            not compiled_action.get("manual_unmodeled_source_effect_required") or
            compiled_action.get("condition_recurring_damage")!=dice or
            compiled_action.get("condition_recurring_damage_type")!=damage_type or
            compiled_action.get("condition_recurring_damage_tick")!=tick or
            not all(phrase.casefold() in text for phrase in phrases) or
            (active_condition in {"poisoned","grappled"} and active_condition not in conditions)):
            errors.append(monster+":"+action_name+":RECURRING_CONTRACT_MISMATCH")
        if recovery_dc and (not re.search(r"\{@actSave\s+con\}\s*\{@dc\s*21",text,re.I) or
                recovery_ability!="con" or auto_rounds!=10):
            errors.append(monster+":"+action_name+":RECURRING_RECOVERY_MISMATCH")
    if set(profile_by_name)!=set(RECURRING_DAMAGE_CONTRACTS):
        errors.append("RECURRING_DAMAGE_CATALOG_MISMATCH")
    return errors,0 if errors else len(RECURRING_DAMAGE_CONTRACTS)

ZERO_HP_CONDITION_CONTRACTS={
    ("Phase Spider","Bite"):("/monster/340/action/1/entries/0","poisoned, paralyzed",True,
        ("reduces the target to 0","becomes {@variantrule Stable|XPHB}","for 1 hour",
         "While {@condition Poisoned|XPHB}","{@condition Paralyzed|XPHB}")),
    ("Vampire Familiar","Umbral Dagger"):("/monster/451/action/1/entries/0","poisoned, paralyzed",True,
        ("reduced to 0","becomes {@variantrule Stable|XPHB}","for 1 hour",
         "While it has the {@condition Poisoned|XPHB}","{@condition Paralyzed|XPHB}")),
}

def check_zero_hp_condition_profiles(source,compiled,profiles):
    source_by_name={m.get("name"):m for m in source};compiled_by_name={m.get("id"):m for m in compiled}
    profile_by_name={(p.get("id"),p.get("action")):p for p in profiles};errors=[]
    for (monster,action_name),(pointer,conditions,stable,phrases) in ZERO_HP_CONDITION_CONTRACTS.items():
        block=source_by_name.get(monster);compiled_block=compiled_by_name.get(monster,{})
        action=next((a for a in (block or {}).get("action",[]) if a.get("name")==action_name),None)
        compiled_action=next((a for a in compiled_block.get("actions",[]) if a.get("name")==action_name),None)
        profile=profile_by_name.get((monster,action_name))
        if action is None or compiled_action is None or profile is None:
            errors.append(monster+":"+action_name+":ZERO_HP_SOURCE_OR_PROFILE_MISSING");continue
        text=" ".join(action.get("entries",[]))
        actual_conditions=", ".join(re.findall(r"\{@condition ([^|}]+)",text,re.I))
        if (profile.get("ptr")!=pointer or profile.get("conditions")!=conditions or
            profile.get("duration_minutes")!=60 or profile.get("stable") is not stable or
            profile.get("linked")!="paralyzed while poisoned" or
            actual_conditions.casefold()!="poisoned, poisoned, paralyzed" or
            not all(phrase.casefold() in text.casefold() for phrase in phrases) or
            not compiled_action.get("source_bound") or
            not compiled_action.get("manual_unmodeled_source_effect_required") or
            compiled_action.get("condition_on_target_reduced_to_zero_hp")!=conditions or
            compiled_action.get("target_stable_when_reduced_to_zero_hp") is not stable):
            errors.append(monster+":"+action_name+":ZERO_HP_CONTRACT_MISMATCH")
    if set(profile_by_name)!=set(ZERO_HP_CONDITION_CONTRACTS):
        errors.append("ZERO_HP_CONDITION_CATALOG_MISMATCH")
    return errors,0 if errors else len(ZERO_HP_CONDITION_CONTRACTS)

def check_unmodeled_action_effects(source,compiled):
    """Require a dedicated fail-closed flag for known untyped source effects."""
    by_id={m["id"]:m for m in compiled}; errors=[]; checked=0; typed_conditions=0
    for monster in source:
        dst=by_id.get(monster.get("name"),{})
        categories=(("action","actions",None),("bonus","special_actions","bonus_action"),
                    ("reaction","special_actions","reaction"),("legendary","special_actions","legendary_action"))
        for source_field,dest_field,category in categories:
            destination=dst.get(dest_field,[])
            if category: destination=[a for a in destination if a.get("category")==category]
            for slot,action in enumerate(monster.get(source_field,[])):
                if not source_action_requires_manual_resolution(action,monster.get("name","")): continue
                checked+=1
                compiled_action=destination[slot] if slot<len(destination) else {}
                expected_name=RCH.sub("",action.get("name","")).strip()
                if compiled_action.get("name")!=expected_name:
                    errors.append(f"{monster['name']}:{action.get('name','?')}:UNTYPED_SOURCE_ACTION_SLOT_MISMATCH")
                if source_action_has_typed_condition_contract(monster['name'],action,compiled_action):
                    typed_conditions+=1
                    continue
                if not compiled_action.get("manual_unmodeled_source_effect_required",False):
                    errors.append(f"{monster['name']}:{action.get('name','?')}:UNTYPED_SOURCE_EFFECT_NOT_MANUAL")
    return errors,checked-typed_conditions

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
    multiattack_plans=java_rows((project/JAVA_MULTIATTACK).read_text(encoding="utf-8"))
    save_condition_profiles=java_rows((project/JAVA_SAVE_CONDITIONS).read_text(encoding="utf-8"))
    recurring_damage_profiles=java_rows((project/JAVA_RECURRING_DAMAGE).read_text(encoding="utf-8"))
    zero_hp_condition_profiles=java_rows((project/JAVA_ZERO_HP_CONDITIONS).read_text(encoding="utf-8"))
    if traitrows!=trait_rows(src):
        raise ValueError("XMM_178_TRAIT_SOURCE_INDEX_MISMATCH")
    expected_usage=usage_rows(src)
    if len(spellrows)!=132 or spellrows!=expected_usage:
        raise ValueError("XMM_132_SPELLCASTING_USAGE_MISMATCH")

    monster_errors,totals=check_monsters(src,index,compiled,attack_contracts)
    branch_errors,branch_totals=check_conditional_attack_damage(src,compiled)
    effect_errors,effect_count=check_unmodeled_action_effects(src,compiled)
    condition_choice_errors,condition_choice_count=check_condition_choice_contracts(src,compiled)
    sphinx_multiattack_errors,sphinx_multiattack_count=check_sphinx_of_secrets_multiattack(src,multiattack_plans)
    werebeast_multiattack_errors,werebeast_multiattack_count=check_werebeast_multiattack_variants(
        src,multiattack_plans)
    compound_multiattack_errors,compound_multiattack_count=check_compound_multiattack_variants(src,multiattack_plans)
    multiattack_catalog_errors,multiattack_structured_count,multiattack_simple_plan_count,multiattack_manual_count=(
        check_multiattack_plan_catalog(src,multiattack_plans))
    save_condition_errors,save_condition_count=check_save_condition_profiles(src,compiled,save_condition_profiles)
    recurring_damage_errors,recurring_damage_count=check_recurring_damage_profiles(
        src,compiled,recurring_damage_profiles)
    zero_hp_condition_errors,zero_hp_condition_count=check_zero_hp_condition_profiles(
        src,compiled,zero_hp_condition_profiles)
    chain_errors=check_chain_devil_infernal_chain(src,compiled)
    attack_damage_errors,attack_damage_count=check_attack_primary_damage(src,compiled)
    multiattack_errors,multiattack_count=check_simple_multiattacks(src,compiled)
    if multiattack_count!=110:
        multiattack_errors.append("EXECUTABLE_SIMPLE_MULTIATTACK_COUNT:"+str(multiattack_count))
    advantage_damage_errors,advantage_damage_count=check_advantage_bonus_attack_damage(src,compiled)
    wounded_advantage_errors,wounded_advantage_count=check_wounded_target_advantage_attacks(src,compiled)
    grappled_advantage_errors,grappled_advantage_count=check_grappled_target_advantage_attacks(src,compiled)
    attack_secondary_errors,attack_secondary_count=check_attack_secondary_damage_contracts(src,compiled)
    turn_start_errors,turn_start_count=check_source_turn_start_hit_conditions(src,compiled)
    save_errors,save_count=check_save_damage_actions(src,save_damage_rows)
    section_errors=check_book(book,bookrows)
    if (monster_errors or branch_errors or effect_errors or condition_choice_errors or sphinx_multiattack_errors or werebeast_multiattack_errors or compound_multiattack_errors or multiattack_catalog_errors or save_condition_errors or recurring_damage_errors or zero_hp_condition_errors or chain_errors or section_errors or save_errors or
        multiattack_errors or
        turn_start_errors or attack_damage_errors or advantage_damage_errors or
        wounded_advantage_errors or grappled_advantage_errors or attack_secondary_errors):
        raise ValueError("XMM_SOURCE_MISMATCH:"+json.dumps(
            {"monsters":monster_errors[:20],"monster_count":len(monster_errors),
             "conditional_attack_damage":branch_errors[:20],"conditional_attack_damage_count":len(branch_errors),
             "unmodeled_action_effects":effect_errors[:20],"unmodeled_action_effects_count":len(effect_errors),
             "condition_choice_contracts":condition_choice_errors,
             "sphinx_multiattack":sphinx_multiattack_errors,
             "werebeast_multiattack":werebeast_multiattack_errors,
             "compound_multiattack":compound_multiattack_errors,
             "multiattack_catalog":multiattack_catalog_errors,
             "save_condition_profiles":save_condition_errors,
             "recurring_damage_profiles":recurring_damage_errors,
             "zero_hp_condition_profiles":zero_hp_condition_errors,
             "chain_devil_infernal_chain":chain_errors,
             "typed_multiattack":multiattack_errors,
             "source_turn_start_conditions":turn_start_errors[:20],"source_turn_start_conditions_count":len(turn_start_errors),
             "attack_primary_damage":attack_damage_errors[:20],"attack_primary_damage_count":len(attack_damage_errors),
             "advantage_bonus_damage":advantage_damage_errors[:20],"advantage_bonus_damage_count":len(advantage_damage_errors),
             "wounded_target_advantage":wounded_advantage_errors[:20],"wounded_target_advantage_count":len(wounded_advantage_errors),
             "grappled_target_advantage":grappled_advantage_errors[:20],"grappled_target_advantage_count":len(grappled_advantage_errors),
             "attack_secondary_damage":attack_secondary_errors[:20],"attack_secondary_damage_count":len(attack_secondary_errors),
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
        "source_attack_primary_damage_compared":attack_damage_count,
        "source_typed_simple_multiattack_contracts_compared":multiattack_count,
        "source_multiattack_plans_compared":306 if not multiattack_catalog_errors else 0,
        "source_simple_multiattack_plan_variants_compared":multiattack_simple_plan_count,
        "source_unstructured_multiattack_plans_manual":multiattack_manual_count,
        "source_attack_advantage_bonus_damage_contracts_compared":advantage_damage_count,
        "source_attack_wounded_target_advantage_contracts_compared":wounded_advantage_count,
        "source_attack_grappled_target_advantage_contracts_compared":grappled_advantage_count,
        "source_attack_secondary_damage_contracts_compared":attack_secondary_count,
        "source_abilities_with_unmodeled_effects_manual":effect_count,
        "source_condition_choice_pure_profiles_compared":condition_choice_count,
        "source_condition_choice_profiles_pending_integration":condition_choice_count,
        "source_complex_multiattack_variants_compared":sphinx_multiattack_count,
        "source_compound_multiattack_variants_compared":compound_multiattack_count,
        "source_werebeast_multiattack_variants_compared":werebeast_multiattack_count,
        "source_manual_save_condition_profiles_compared":save_condition_count,
        "source_manual_save_condition_profiles_pending_integration":save_condition_count,
        "source_manual_recurring_damage_profiles_compared":recurring_damage_count,
        "source_manual_recurring_damage_profiles_pending_integration":recurring_damage_count,
        "source_manual_zero_hp_condition_profiles_compared":zero_hp_condition_count,
        "source_manual_zero_hp_condition_profiles_pending_integration":zero_hp_condition_count,
        "source_chain_devil_infernal_chain_semantics_compared":1,
        "source_compound_condition_actions_typed":2,
        "source_single_condition_actions_typed":9+turn_start_count,
        "source_timed_noncondition_effects_typed":1,
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
