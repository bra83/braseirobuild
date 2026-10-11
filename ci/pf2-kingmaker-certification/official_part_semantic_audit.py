#!/usr/bin/env python3
"""Conservative official part-title semantic crosscheck against published Kingmaker source.
Not a semantic correctness certificate: untranslated titles are a weak presence signal.
Missing English titles can be legitimate PT-BR translations, so gaps are review items.
"""
import argparse, json, re, unicodedata
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument("--project",type=Path,required=True);p.add_argument("--out",type=Path,required=True)
args=p.parse_args()
root=args.project.resolve()
out=args.out.resolve();out.mkdir(parents=True,exist_ok=True)
book=json.loads((Path(__file__).parent/"KINGMAKER_CANONICAL_BOOK_CROSSWALK.json").read_text(encoding="utf-8"))
chapterdir=root/"android/app/src/main/assets/pathfinder/kingmaker/chapters"
sources={x.name:x.read_text(encoding="utf-8") for x in chapterdir.glob("chapter_*.json")}
def norm(s):
    return re.sub(r'[^a-z0-9]+',' ',unicodedata.normalize("NFKD",s).encode("ascii","ignore").decode().lower()).strip()
stop={"the","of","and","a","to","into","at","for","from","part","on","in","an","run","it"}
entries=[];missing=0
for ch in book["official_adventure_chapters"]:
    candidate=[]
    for stem in ch["candidate_assets"]:
        bare=stem.removesuffix(".json")
        candidate.extend(x for x in sources if x==bare+".json" or x.startswith(bare+"_"))
    candidate=sorted(set(candidate))
    blob=norm("\n".join(sources[k] for k in candidate))
    parts=[]
    for name in ch.get("parts",[]):
        label=norm(name)
        if label in blob:state="literal_phrase_present"
        else:
            tokens=[x for x in label.split() if x not in stop and len(x)>3]
            state="keyword_candidate" if tokens and all(t in blob for t in tokens) else "requires_manual_page_comparison"
        if state=="requires_manual_page_comparison":missing+=1
        parts.append({"name":name,"status":state})
    entries.append({"chapter":ch["number"],"title":ch["name"],"parts":parts,"assets":candidate})
result={"source":"Kingmaker.pdf official contents + R35 JSON catalogs","chapters":entries,
        "review_required_parts":missing,
        "caveat":"Literal or keyword matches do not prove mechanical/plot fidelity; absence can be translation. Page-by-page certification still required."}
(out/"OFFICIAL_PART_SEMANTIC_SIGNALS.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
lines=["## Official chapter part-title correspondence signals"]
for row in entries:
    lines.append(f"### {row['chapter']}. {row['title']}")
    for part in row["parts"]:lines.append(f"- {part['name']}: {part['status']}")
lines+=["",f"**Manual review needed for {missing} official part names.**",
        "Signals are not a whole-campaign certification."]
(out/"OFFICIAL_PART_SEMANTIC_SIGNALS.md").write_text("\n".join(lines)+"\n",encoding="utf-8")
print("OFFICIAL_PART_AUDIT="+json.dumps({"chapters":len(entries),"review_needed":missing,"part_names":sum(len(x["parts"]) for x in entries)}))
for row in entries:
    for part in row["parts"]:
        if part["status"]=="requires_manual_page_comparison":
            print(f"OFFICIAL_PART_REVIEW chapter={row['chapter']} title={part['name']}")
