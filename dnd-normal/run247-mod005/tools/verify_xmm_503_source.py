#!/usr/bin/env python3
"""XMM-2025: compare every compiled and indexed monster against independent pinned
source JSON, plus the 36 source-bound book sections. Never certify just a class count.
No original statblock prose is republished in the Android app.
"""
import argparse, hashlib, json, re, urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
PIN="8c026b807fac21862a309379a0c3228a0683198b"
BESTIARY_SHA="9a4137b508ef37562ed948894a70b1594b57e85c"
BOOK_SHA="a21801793df3515d07ef9a56d9febefea1c6f8f7"
FILES=("data/bestiary/bestiary-xmm.json","data/book/book-xmm.json")
SOURCE="https://raw.githubusercontent.com/5etools-mirror-3/5etools-src/"+PIN+"/"
JAVA_INDEX="app/src/main/java/com/braseiro/dnd5e/Xmm2025SourceIndex.java"
JAVA_BOOK="app/src/main/java/com/braseiro/dnd5e/Xmm2025BookIndex.java"
JAVA_RUNTIME="app/src/main/java/com/braseiro/dnd5e/Xmm2025CompiledBestiaryEngine.java"
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

def check_monsters(source,index,compiled):
    problems=[]
    if len(source)!=503 or len(index)!=503 or len(compiled)!=503:
        raise ValueError(f"XMM_NOT_503:{len(source)}:{len(index)}:{len(compiled)}")
    if len({x["name"] for x in source})!=503 or len({x["id"] for x in index})!=503:
        raise ValueError("XMM_DUPLICATE_MONSTERS")
    recharges,spellcasters=0,0
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
                     "spellcasting_monsters":spellcasters}

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

def check_book(book,rows):
    problems=[]
    if len(rows)!=36 or len({r["key"] for r in rows})!=36:
        return ["XMM_36_SECTION_COUNT"]
    sections=book["bookData"][0]["data"]
    chapters=[0,1,2,3,4]
    for r in rows:
        k=r["key"]
        try:ref=deref(book,r["pointer"])
        except (IndexError,KeyError,TypeError):problems.append(k+":INVALID_POINTER");continue
        if r["title"]!=ref.get("name"):problems.append(k+":TITLE_DIFF")
        if k.startswith("ch_1:"):
            if r["names"]!=creature_refs(ref):problems.append(k+":ALPHABET_CREATURE_NAMES")
        if k=="ch_2:sec_2_0":
            expected=re.findall(r'\{@creature ([^|}]+)',json.dumps(ref["entries"][2]["items"]))
            if r["names"]!=expected:problems.append(k+":ANIMAL_NAMES")
        if k=="ch_3:sec_3_0" and len(r["conversion_rows"])!=59:
            problems.append(k+":CONVERSIONS")
        if k=="ch_3:sec_3_1" and len(r["groups"])!=12:problems.append(k+":HABITATS")
        if k=="ch_3:sec_3_2" and len(r["groups"])!=14:problems.append(k+":CREATURE_TYPES")
        if k=="ch_3:sec_3_3" and len(r["groups"])!=12:problems.append(k+":GROUPS")
        if k=="ch_3:sec_3_4" and len(r["groups"])!=30:problems.append(k+":CHALLENGE_LISTS")
        if k=="ch_4:sec_4_0" and not r.get("editorial_only"):problems.append(k+":CREDITS_NOT_EDITORIAL")
    return problems

def check(project, bestiary_bytes, book_bytes):
    if blob_sha(bestiary_bytes)!=BESTIARY_SHA:raise ValueError("XMM_BESTIARY_GIT_BLOB_MISMATCH")
    if blob_sha(book_bytes)!=BOOK_SHA:raise ValueError("XMM_BOOK_GIT_BLOB_MISMATCH")
    src=json.loads(bestiary_bytes)["monster"]
    book=json.loads(book_bytes)
    index=java_rows((project/JAVA_INDEX).read_text(encoding="utf-8"))
    compiled=java_rows((project/JAVA_RUNTIME).read_text(encoding="utf-8"))
    bookrows=java_rows((project/JAVA_BOOK).read_text(encoding="utf-8"))
    monster_errors,totals=check_monsters(src,index,compiled)
    section_errors=check_book(book,bookrows)
    if monster_errors or section_errors:
        raise ValueError("XMM_SOURCE_MISMATCH:"+json.dumps(
            {"monsters":monster_errors[:20],"monster_count":len(monster_errors),
             "sections":section_errors[:20],"section_count":len(section_errors)}))
    return {"format":"XMM_INDEPENDENT_SOURCE_MATCH_V1","source_commit":PIN,
        "bestiary_git_blob_sha1":BESTIARY_SHA,"book_git_blob_sha1":BOOK_SHA,
        "source_monsters_compared":503,"editorial_sections_compared":36,
        "recharge_actions_compared":totals["recharge_actions"],
        "spellcaster_records_compared":totals["spellcasting_monsters"],
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
        files=[]
        for name in FILES:
            req=urllib.request.Request(SOURCE+name,headers={"User-Agent":"Braseiro-XMM-Provenance/1.0"})
            with urllib.request.urlopen(req,timeout=90) as f:files.append(f.read(5*1024*1024))
        best,book=files
    result=check(args.project_root.resolve(),best,book)
    payload=json.dumps(result,indent=2,ensure_ascii=False)
    print(payload)
    if args.json_out:
        args.json_out.parent.mkdir(parents=True,exist_ok=True)
        args.json_out.write_text(payload+"\n",encoding="utf-8")
if __name__=="__main__":main()
