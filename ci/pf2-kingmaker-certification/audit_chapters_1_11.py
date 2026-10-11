#!/usr/bin/env python3
"""Kingmaker 2e: evidence-based audit of ALL 11 official adventure chapters.

Input MUST be an extracted real Android source project. Compare explicit proper
nouns/locations and quest dependency edges with the official PDF's chapter map.
This deliberately does NOT award semantic certification for name matches.

Sources: Kingmaker.pdf pp.14–501 (Drive 1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3)
and KINGMAKER_CANONICAL_BOOK_CROSSWALK.json. R35 is immutable.
"""
import argparse,collections,hashlib,json,re,unicodedata
from pathlib import Path

SOURCE_ID="1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3"
# Canonical story entities checked against official AP chapter text, not vague
# title overlap. PT-BR aliases avoid penalizing translated source.
BEATS={
  1:[("Jamandi Aldori",("jamandi",)),("Black Tears",("black tears","lagrimas negras"))],
  2:[("Old Sycamore",("sycamore","sicomoro")),("Candlemere",("candlemere",))],
  3:[("Oleg's Trading Post",("oleg",)),("Stag Lord",("stag lord","senhor dos cervos","senhor do cervo"))],
  4:[("Hargulka",("hargulka",))],
  5:[("Lamashtu bloom",("lamashtu",)),("Darivan",("darivan",))],
  6:[("Vordakai",("vordakai",)),("Nomen centaurs",("nomen",))],
  7:[("Fort Drelev",("drelev",)),("Armag",("armag",)),("Tatzlford",("tatzlford",))],
  8:[("Rushlight Festival",("rushlight",)),("Castruccio Irovetti",("irovetti",)),("Whiterose",("whiterose","rosa branca"))],
  9:[("Foras, Thresholders",("foras",)),("Yog-Sothoth",("yog sothoth","yog-sothoth")),("Candlemere subterranean complex",("candlemere",))],
  10:[("Nyrissa",("nyrissa",)),("House at the Edge of Time",("casa no limiar do tempo","house at the edge of time")),("Thousandbreaths",("thousandbreaths","mil respiros"))],
  11:[("Nyrissa resolution",("nyrissa",)),("The Apology",("apology","apologia")),("Hunting Dream",("hunting dream","sonho de caca","sonho da caca")),("Lantern Kingdom",("lantern kingdom","reino da lanterna"))],
}

def norm(x):
    x=unicodedata.normalize("NFKD",str(x).casefold())
    return re.sub(r'[^a-z0-9]+',' ',''.join(c for c in x if not unicodedata.combining(c))).strip()

def candidates(ch,files):
    result=[]
    for stem in ch["candidate_assets"]:
        t=stem.removesuffix(".json")
        result.extend(f for f in files if f==t+".json" or f.startswith(t+"_"))
    return sorted(set(result))

def audit(root):
    book=json.loads((Path(__file__).with_name("KINGMAKER_CANONICAL_BOOK_CROSSWALK.json")).read_text(encoding="utf-8"))
    assert book["official_sources"]["kingmaker_pdf_id"]==SOURCE_ID if isinstance(book.get("official_sources"),dict) and "kingmaker_pdf_id" in book["official_sources"] else True
    chapters=book["official_adventure_chapters"]
    if len(chapters)!=11 or sum(len(c["parts"]) for c in chapters)!=40:
        raise ValueError("Official 11-chapter/40-part crosswalk drift")
    directory=Path(root)/"android/app/src/main/assets/pathfinder/kingmaker/chapters"
    files={p.name:json.loads(p.read_text(encoding="utf-8")) for p in directory.glob("chapter_*.json")}
    if len(files)<11:raise RuntimeError("Unexpected incomplete campaign source")
    issues=[];results=[]
    global_quests={q.get("quest_key"):(name,q) for name,d in files.items() for q in d.get("quests",[]) if isinstance(q,dict) and q.get("quest_key")}
    crossedges=[]
    for filename,doc in files.items():
        for q in doc.get("quests",[]):
            if not isinstance(q,dict):continue
            for dependency in q.get("prerequisites",[]):
                other=global_quests.get(dependency)
                if other is None:
                    issues.append({"severity":"critical","code":"DANGLING_QUEST_DEPENDENCY","quest":q.get("quest_key"),"dependency":dependency})
                elif other[0]!=filename:
                    crossedges.append({"previous_asset":other[0],"next_asset":filename,"previous_quest":dependency,"next_quest":q.get("quest_key")})
    for chapter in chapters:
        number=chapter["number"]
        names=candidates(chapter,files)
        if not names:
            issues.append({"severity":"critical","code":"MISSING_CHAPTER_CANDIDATE","chapter":number})
        typed={kind:[(filename,row) for filename in names for row in files[filename].get(kind,[]) if isinstance(row,dict)] for kind in ("quests","scenes","npcs","pois","events","milestones","hazards")}
        # Narrative evidence must be in structured gameplay records, not a book
        # name copied into a metadata chapter heading.
        gameplay=norm(json.dumps(typed,ensure_ascii=False))
        anchor_results=[]
        for name,aliases in BEATS[number]:
            found=any(norm(alias) in gameplay for alias in aliases)
            anchor_results.append({"official_anchor":name,"present_in_playable_records":found,"aliases_checked":list(aliases)})
            if not found:issues.append({"severity":"review","code":"OFFICIAL_PLOT_ANCHOR_MISSING","chapter":number,"anchor":name})
        # Official part title signals are weak; mark every part as requiring
        # consequence and page-level comparison, even when words appear.
        parts=[{"title":part,"title_keyword_signal":any(norm(w) in gameplay for w in norm(part).split() if len(w)>=6),
                "page_mechanics_and_branches_verified":False} for part in chapter["parts"]]
        results.append({"chapter":number,"official_title":chapter["name"],"official_page":chapter["page"],
                        "assets":names,"typed_record_counts":{k:len(v) for k,v in typed.items()},
                        "anchors":anchor_results,"parts":parts,
                        "playable_branch_certified":False,"official_fidelity_certified":False})
    chapter9=[f for f in results if f["chapter"]==9][0]
    for file in chapter9["assets"]:
        for q in files[file].get("quests",[]):
            title=norm(q.get("title",""))
            if "neogi" in title or "devoradores de mentes" in title:
                issues.append({"severity":"critical","code":"NONCANONICAL_CHAPTER_9_QUEST",
                               "asset":file,"quest":q.get("quest_key"),
                               "reason":"Official chapter centers on Foras / Thresholders / Yog-Sothoth beneath Candlemere"})
    chapter11_files=[f for f in results if f["chapter"]==11][0]["assets"]
    endings=global_quests.get("q_redencao_espada_briar")
    chapter11_gate=global_quests.get("q_revelacao_rei_lanterna")
    transition_ok=bool(endings and chapter11_gate and endings[0]!=chapter11_gate[0] and
                       endings[1].get("quest_key") in chapter11_gate[1].get("prerequisites",[]))
    if not transition_ok:
        issues.append({"severity":"critical","code":"CHAPTER_10_11_UNLOCKS_WITHOUT_NYRISSA_RESOLUTION",
                       "hint":"The Lantern King must follow Nyrissa slain/forgiven (Kingmaker p.480)"})
    counts=collections.Counter(x["severity"] for x in issues)
    return {"schema":"kingmaker-official-chapters-1-11-evidence-v1",
            "official_source_pdf_id":SOURCE_ID,"data_source":"actual project Kingmaker chapter JSON",
            "numbered_official_chapters":11,"official_story_parts":40,"assets_inspected":len(files),
            "chapter_10_to_11_prerequisite_verified":transition_ok,
            "cross_asset_quest_dependency_edges":crossedges,
            "findings":issues,"finding_counts":dict(counts),"chapters":results,
            "whole_campaign_certified":False,
            "limitations":["Canonical named anchors are necessary, not sufficient","No chapter is certified by title presence",
                           "No autonomous full-game Android playthrough or outcome persistence was executed",
                           "Page-by-page encounter/choice/mechanic implementation remains separate"]}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--project",required=True)
    p.add_argument("--out",required=True)
    p.add_argument("--fail-on-critical",action="store_true")
    a=p.parse_args()
    data=audit(a.project)
    path=Path(a.out);path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    lines=["# Kingmaker 2e — capítulos oficiais 1–11","",
           "Fonte: Kingmaker.pdf pp.14–501 / ID "+SOURCE_ID,
           "Fonte runtime: projeto Android real, arquivos JSON. **NÃO CERTIFICADO**.",""]
    for ch in data["chapters"]:
        ok=sum(x["present_in_playable_records"] for x in ch["anchors"])
        lines.append("## "+str(ch["chapter"])+". "+ch["official_title"]+
                     " — "+str(ok)+"/"+str(len(ch["anchors"]))+" entidades centrais localizadas")
        lines.append("Assets: "+", ".join(ch["assets"]))
        for anchor in ch["anchors"]:
            lines.append("- "+("ENCONTRADO" if anchor["present_in_playable_records"] else "FALTA")+
                         ": "+anchor["official_anchor"])
        lines.append("Partes oficiais: "+str(len(ch["parts"]))+"; homologação de escolhas/consequências: PENDENTE")
    lines.extend(["","## Bloqueios materiais",""])
    for i in data["findings"]:
        lines.append("- "+str(i["severity"]).upper()+" — "+i["code"]+": "+str({k:v for k,v in i.items() if k not in ("severity","code")}))
    lines.extend(["","## Progressão","Dependências entre arquivos: "+str(len(data["cross_asset_quest_dependency_edges"])),
                  "Capítulo 10→11 exige resolução de Nyrissa: "+str(data["chapter_10_to_11_prerequisite_verified"]),
                  "**Não considerar as 40 partes homologadas sem confrontar escolhas, DCs, criaturas, encontros e finais contra as páginas oficiais.**"])
    report="\n".join(lines)+"\n"
    (path.parent/"KINGMAKER_CHAPTERS_1_11_REVIEW.md").write_text(report,encoding="utf-8")
    print("KINGMAKER_CHAPTERS_11_AUDIT="+json.dumps({"chapters":11,"parts":40,"missing_named_anchors":sum(i["code"]=="OFFICIAL_PLOT_ANCHOR_MISSING" for i in data["findings"]),
        "critical":data["finding_counts"].get("critical",0),"cross_chapter_edges":len(data["cross_asset_quest_dependency_edges"]),
        "nyrissa_gate":data["chapter_10_to_11_prerequisite_verified"]},ensure_ascii=False))
    for x in data["findings"]: print("KINGMAKER_CHAPTER_AUDIT_FINDING="+json.dumps(x,ensure_ascii=False))
    if a.fail_on_critical and data["finding_counts"].get("critical",0):raise SystemExit(2)

if __name__=="__main__":
    main()
