#!/usr/bin/env python3
"""R36: materialize missing official structure catalog definitions in production Java.

Inputs are the pinned R35->R36 source and the existing 73-entry Kingmaker.pdf
extraction. The 18 already-corrected entries are not overwritten.
This is catalog/cost/DC plumbing, NOT full certification of printed special effects.
Ambiguous payment or no-lot structures are explicitly rejected by the current
Build Structure resolver rather than silently charging fabricated costs.
"""
from pathlib import Path
import json
import re
import sys
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "pf2-kingmaker-certification" / "kingmaker_official_structures_p544_552.json"
RULES = Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRulesEngine.java")
REPO = Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java")
PREFIX = "R36_OFFICIAL_CATALOG_73"
KEY_OVERRIDES = {"ALCHEMY LABORATORY": "alchemist", "STABLE": "stables"}

def key_for(name):
    if name in KEY_OVERRIDES:
        return KEY_OVERRIDES[name]
    ascii_name = unicodedata.normalize("NFKD", name).encode("ascii","ignore").decode("ascii")
    return re.sub(r"(^_+|_+$)","",re.sub(r"[^a-z0-9]+","_",ascii_name.lower()))

def skill_data(value):
    pairs = re.findall(r"([A-Za-z]+)(?: \((trained|expert|master|legendary)\))?",value)
    if not pairs:
        raise ValueError("No official construction skill: "+value)
    skills = [p[0].lower() for p in pairs]
    if len(set(skills))!=len(skills):
        raise ValueError("Repeated construction skills: "+value)
    ranks = set(p[1] or "untrained" for p in pairs)
    if len(ranks)!=1:
        raise ValueError("Mixed ranks need review: "+value)
    return skills,ranks.pop()

def official_manifest():
    data=json.loads(MANIFEST.read_text(encoding="utf-8"))
    if data.get("source_drive_file_id")!="1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3" or len(data.get("structures",[]))!=73:
        raise ValueError("Kingmaker PDF manifest identity/count drift")
    keys=[key_for(x["official_name"]) for x in data["structures"]]
    if len(set(keys))!=73:
        raise ValueError("Duplicate canonical keys in 73-entry manifest")
    return data["structures"]

def java_quote(value):
    return json.dumps(str(value),ensure_ascii=True)

def construct_expression(row,canonical):
    skills,rank=skill_data(row["construction"])
    field_values = [
       ("name",row["official_name"]),("level",row["official_level"]),
       ("lots",row["lots"] if row["lots"] is not None else 0),
       ("rp",row["costs"]["rp"]),("lumber",row["costs"]["lumber"]),
       ("stone",row["costs"]["stone"]),("ore",row["costs"]["ore"]),
       ("luxuries",row["costs"]["luxuries"]),
       ("construction_rank",rank),("construction_dc",row["construction_dc"]),
       ("official_source",row["source"]),("official_catalog_key",canonical),
    ]
    if len(skills)==1:field_values.append(("construction_skill",skills[0]))
    else:field_values.append(("construction_skills",",".join(skills)))
    if row["lots"] is None:field_values.append(("requires_special_placement",True))
    if " or " in row["cost_text"]:
        field_values += [("requires_cost_choice",True),
                         ("official_alternative_cost_text",row["cost_text"])]
    # Special traits/effects are NOT automatically inferred from numeric entries.
    field_values.append(("special_effects_semantic_certification","PENDING"))
    expr="new JSONObject()"
    for k,value in field_values:
        if isinstance(value,bool):j="true" if value else "false"
        elif isinstance(value,int):j=str(value)
        else:j=java_quote(value)
        expr += ".put("+java_quote(k)+","+j+")"
    return expr

def transform_rules(source,rows):
    if PREFIX in source:raise ValueError("Catalog expansion applied twice")
    header="public JSONObject structure(String key)"
    if source.count(header)!=1:raise ValueError("KingmakerRulesEngine.structure source drift")
    begin=source.index(header)
    opening=source.index("{",begin+len(header))
    end=source.find("\n    public ",opening+1)
    if end<0:raise ValueError("Cannot locate catalog method end")
    old_method=source[opening:end]
    # Existing corrected catalog keys must remain authoritative.
    existing=set(re.findall(r'if\s*\(\s*"([^"]+)"\.equals\(k\)\s*\)',old_method))
    if not existing:raise ValueError("No original structure definitions")
    generated=[]
    for row in rows:
        canonical=key_for(row["official_name"])
        if canonical in existing:continue
        expr=construct_expression(row,canonical)
        generated.append('        if("'+canonical+'".equals(r36OfficialKey)) return '+expr+';\n')
    if not generated:raise ValueError("No missing structure entries found; inspect source")
    addition=(" \n        // "+PREFIX+"; do not replace existing 18 official corrections.\n"
      '        String r36OfficialKey=key==null?"":key.trim().toLowerCase(java.util.Locale.US)\n'
      '            .replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");\n'
      '        try {\n'+''.join(generated)
      '        } catch(Exception officialCatalogFailure) {\n'
      '            throw new IllegalStateException("Kingmaker official catalog failure",officialCatalogFailure);\n'
      '        }\n')
    return source[:opening+1]+addition+source[opening+1:],{"existing":sorted(existing),"generated":[key_for(r["official_name"]) for r in rows if key_for(r["official_name"]) not in existing]}

def transform_repository(source):
    # Fail closed on types whose source manifest does not identify a normal
    # urban-grid lot allocation, or for which alternative commodity tender
    # has not been selected. Otherwise the existing code could silently
    # bill its default commodity fields as if they were the only official option.
    anchor='if(town.length()==0||def==null)throw new IllegalArgumentException("Assentamento ou estrutura inválidos.");'
    if source.count(anchor)!=2:raise ValueError("Build and Repair resolver drift")
    guard=(anchor+'\n'
           '        if(def.optBoolean("requires_special_placement",false))\n'
           '            throw new IllegalStateException("Estrutura de implantação especial: selecione o tipo/local exigido pelo manual Kingmaker.");\n'
           '        if(def.optBoolean("requires_cost_choice",false))\n'
           '            throw new IllegalStateException("Esta estrutura exige escolha explícita de commodity alternativa antes de cobrar RP.");')
    source=source.replace(anchor,guard)
    return source

def apply(directory):
    root=Path(directory).resolve()
    v=root/"version.properties"
    if not v.is_file() or "VERSION_NAME=9.0.37" not in v.read_text(encoding="utf-8"):
        raise ValueError("Only R36 verified project eligible")
    rows=official_manifest()
    p=root/RULES
    rp=root/REPO
    rules=p.read_text(encoding="utf-8")
    repository=rp.read_text(encoding="utf-8")
    adjusted,evidence=transform_rules(rules,rows)
    adjusted_repo=transform_repository(repository)
    # Atomic-ish: all transformations checked before mutating either file.
    p.write_text(adjusted,encoding="utf-8")
    rp.write_text(adjusted_repo,encoding="utf-8")
    print("KINGMAKER_R36_CATALOG_EXPANSION="+json.dumps(
       {"official_rows":len(rows),"generated_missing":len(evidence["generated"]),
        "existing_kept":len(rows)-len(evidence["generated"]),
        "generated_keys":evidence["generated"],"numeric_plus_skill_dc_in_source":True,
        "special_effects_semantic_certification":"OPEN",
        "alternative_cost_choice":"FAIL_CLOSED"},
       ensure_ascii=False),flush=True)

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_catalog_expansion.py <R36_project>")
    apply(sys.argv[1])
