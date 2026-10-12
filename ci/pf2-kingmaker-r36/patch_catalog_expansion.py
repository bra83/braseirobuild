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
SEMANTICS = ROOT / "pf2-kingmaker-certification" / "kingmaker_official_structure_effects_p544_552.json"
KEY_OVERRIDES = {"ALCHEMY LABORATORY": "alchemist", "STABLE": "stables"}

def key_for(name):
    if name in KEY_OVERRIDES:
        return KEY_OVERRIDES[name]
    ascii_name = unicodedata.normalize("NFKD", name).encode("ascii","ignore").decode("ascii")
    return re.sub(r"(^_+|_+$)","",re.sub(r"[^a-z0-9]+","_",ascii_name.lower()))

def skill_data(value):
    pieces = re.split(r",\s*(?:or\s+)?|\s+or\s+",value.strip())
    pairs = []
    for piece in pieces:
        match=re.fullmatch(r"([A-Za-z]+)(?: \((trained|expert|master|legendary)\))?",piece.strip())
        if not match:raise ValueError("Unknown official construction option: "+piece)
        pairs.append(match.groups())
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

def official_semantics():
    data=json.loads(SEMANTICS.read_text(encoding="utf-8"))
    if data.get("certification")!="NOT_CERTIFIED" or data.get("source_drive_file_id")!="1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3":
        raise ValueError("Structure effects provenance/certification drift")
    records=data.get("structures",[])
    if len(records)!=73:raise ValueError("Official traits/effects crosswalk must contain 73 entries")
    result={}
    for rec in records:
        canonical=key_for(rec["official_name"])
        if canonical in result:raise ValueError("Duplicate structure effect metadata key: "+canonical)
        result[canonical]=rec
    if set(result)!=set(key_for(row["official_name"]) for row in official_manifest()):
        raise ValueError("Structure effect metadata does not exactly match 73 official catalog keys")
    return result

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
    existing=set(re.findall(r'if\s*\(\s*"([^"]+)"\.equals\(k\)\s*\)',old_method))
    # The 18 separately audited/corrected definitions are never overwritten.
    certified_prior={"academy","alchemist","bank","barracks","brewery",
       "castle","cathedral","embassy","garrison","granary","library",
       "monument","pier","smithy","stables","temple","theater","town_hall"}
    if not certified_prior.issubset(existing):
        raise ValueError("Canonical 18 previously patched official catalog keys are missing")
    effects=official_semantics()
    # Every stat block is mapped from the actual PDF. Apply mechanical
    # RESIDENTIAL lots now: refreshSettlementOccupancy() reads this field from
    # each built structure's effects_json. Other clauses are retained as exact
    # official source metadata, NOT falsely treated as implemented mechanics.
    semantic_overlay=[]
    for row in rows:
        canonical=key_for(row["official_name"])
        rec=effects[canonical]
        residence=1 if "RESIDENTIAL" in rec["traits"] else 0
        statement='            if("'+canonical+'".equals(r36OfficialKey)){\\n'
        for field,value in (
            ("residential_lots",residence),
            ("official_traits",",".join(rec["traits"])),
            ("official_item_bonus_rule",rec["item_bonus_rule"]),
            ("official_ruin_rule",rec["ruin_rule"]),
            ("official_effects_rule",rec["effects_rule"]),
            ("official_upgrade_from",rec["upgrade_from"]),
            ("official_upgrade_to",rec["upgrade_to"]),
        ):
            literal=str(value) if isinstance(value,int) else java_quote(value)
            statement+='                officialBase.put('+java_quote(field)+','+literal+');\\n'
        statement+='            }\\n'
        semantic_overlay.append(statement)
    if len(semantic_overlay)!=73:
        raise ValueError("73 official structure semantics not overlaid")
    generated=[]
    for row in rows:
        canonical=key_for(row["official_name"])
        if canonical in certified_prior:continue
        expr=construct_expression(row,canonical).replace("new JSONObject()","officialBase",1)
        # Correct the numeric rules without wiping any preexisting special
        # effects, prerequisites, residential lots, or other runtime metadata.
        generated.append('            if("'+canonical+'".equals(r36OfficialKey)) {\n'
          '                officialBase.put("construction_skill","");\n'
          '                officialBase.put("construction_skills","");\n'
          '                return '+expr+';\n'
          '            }\n')
    if len(generated)!=55:
        raise ValueError("Expected exactly 55 overlays after canonical 18")
    wrapper=('    // '+PREFIX+': enrich/preserve baseline effects; replace only official fields.\n'
      '    public JSONObject structure(String key){\n'
      '        JSONObject officialBase=structureR35Baseline(key);\n'
      '        boolean hadR35Structure=officialBase!=null && officialBase.length()>0;\n'
      '        if(officialBase==null)officialBase=new JSONObject();\n'
      '        String r36OfficialKey=key==null?"":key.trim().toLowerCase(java.util.Locale.US)\n'
      '            .replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");\n'
      '        try {\n'+''.join(semantic_overlay)+''.join(generated)+
      '        } catch(Exception officialCatalogFailure) {\n'
      '            throw new IllegalStateException("Kingmaker official catalog failure",officialCatalogFailure);\n'
      '        }\n'
      '        // Invalid keys must still be rejected, never free 0-RP buildings.\n'
      '        return hadR35Structure?officialBase:null;\n'
      '    }\n'
      '    ')
    original=source[:begin]+header.replace("public JSONObject structure","private JSONObject structureR35Baseline")+source[begin+len(header):]
    updated=original[:begin]+wrapper+original[begin:]
    return updated,{"existing":sorted(existing),
                    "generated":[key_for(r["official_name"]) for r in rows
                                 if key_for(r["official_name"]) not in certified_prior]}

def transform_repository(source):
    # Both existing public 5-argument paths remain ABI-compatible. The
    # optional sixth argument is an explicit Lumber/Stone tender chosen by
    # the player. Never charge BOTH alternative commodities.
    methods=(
      "resolveBuildStructure","resolveRepairStructure",
    )
    for method in methods:
        old="public JSONObject "+method+"(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes)throws Exception{"
        if source.count(old)!=1:
            raise ValueError(method+" source signature drift")
        wrapper=("public JSONObject "+method+
          "(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes)throws Exception{\n"
          "        return "+method+"(settlementKey,structureKey,naturalRoll,skillKey,notes,\"\");\n"
          "    }\n"
          "    public JSONObject "+method+
          "(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice)throws Exception{")
        source=source.replace(old,wrapper)
    anchor='if(town.length()==0||def==null)throw new IllegalArgumentException("Assentamento ou estrutura inválidos.");'
    if source.count(anchor)!=2:
        raise ValueError("Build and Repair resolver drift")
    guard=(anchor+'\n'
           '        if(def.optBoolean("requires_special_placement",false))\n'
           '            throw new IllegalStateException("Estrutura de implantação especial: selecione o tipo/local exigido pelo manual Kingmaker.");\n'
           '        if(!def.optBoolean("requires_cost_choice",false)&&commodityChoice.length()>0)\n'
           '            throw new IllegalArgumentException("Esta estrutura não tem custo alternativo.");\n'
           '        if(def.optBoolean("requires_cost_choice",false)){\n'
           '            if("lumber".equals(commodityChoice))def.put("stone",0);\n'
           '            else if("stone".equals(commodityChoice))def.put("lumber",0);\n'
           '            else throw new IllegalStateException("Escolha pagar a commodity alternativa com Madeira ou Pedra.");\n'
           '        }')
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
