#!/usr/bin/env python3
"""Independent R35 content/continuity endurance audit.
Reads the actual compiled source's JSON; never generates fake chapters.
The file-backed SQLite test verifies the audit harness and asset lifecycle,
NOT Android AppDb or phone-level gameplay.
"""
from __future__ import annotations
import argparse, collections, hashlib, json, os, sqlite3, tempfile, time, traceback
from pathlib import Path

ap = argparse.ArgumentParser()
ap.add_argument("--project", type=Path, required=True)
ap.add_argument("--out", type=Path, required=True)
args = ap.parse_args()
root = args.project.resolve()
out = args.out.resolve()
out.mkdir(parents=True, exist_ok=True)
asset = root / "android/app/src/main/assets/pathfinder/kingmaker/chapters"
issues, metrics, paths, chapters = [], {}, {}, {}
def finding(severity, code, message, **extra):
    issues.append({"severity":severity,"code":code,"description":message,**extra})
def require(cond, code, message, **extra):
    if not cond: finding("critical",code,message,**extra)
for i in range(13):
    matches = list(asset.glob(f"chapter_{i:02d}_*.json"))
    require(len(matches)==1,"CHAPTER_ASSET_MISSING_OR_DUPLICATE",f"chapter_{i:02d}: found {len(matches)} sources",files=[str(p.name) for p in matches])
    if len(matches)!=1: continue
    path = matches[0]
    try: doc = json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:
        finding("critical","CHAPTER_INVALID_JSON",f"{path.name}: {e}")
        continue
    chapters[i] = doc
    paths[i] = path.name
    require(doc.get("chapter_number")==i,"CHAPTER_NUMBER_MISMATCH",path.name,actual=doc.get("chapter_number"))
    require(doc.get("locale")=="pt-BR","LOCALE_MISMATCH",path.name)
    require(doc.get("schema")=="braseiro-pathfinder2e-chapter-v1","SCHEMA_MISMATCH",path.name)
    for field in ("quests","hazards","pois","npcs","scenes","events","milestones"):
        # Chapter 00 is an urban prelude. The empty tactical_scenes alias
        # is optional; production ChapterCatalog uses optJSONArray("scenes").
        if i==0 and field=="scenes" and "scenes" not in doc: continue
        require(isinstance(doc.get(field),list),"CHAPTER_COLLECTION_INVALID",f"{path.name}: {field}")
if len(chapters)<13: finding("critical","CAMPAIGN_NOT_COMPLETE","Not all 13 prologue+chapter JSONs are readable")
# Note: 11 (atlas) and 12 (companions) are supplements, NOT sequential plot episodes.
plot=list(range(1,11))
metrics["asset_count"]=len(chapters)
metrics["plot_chapters"]=len([x for x in plot if x in chapters])
metrics["supplemental_compendiums"]=[paths.get(11),paths.get(12)]
metrics["source_identity"]={str(i):{"title":d.get("title"),"sha256":hashlib.sha256((asset/paths[i]).read_bytes()).hexdigest()} for i,d in chapters.items()}
collections_by_kind={"quests":"quest_key","pois":"poi_key","npcs":"npc_key","scenes":"scene_key","hazards":"hazard_key","events":"event_key","milestones":"milestone_key"}
all_records={kind:[] for kind in collections_by_kind}
for n,d in chapters.items():
    for kind,field in collections_by_kind.items():
        for row in d.get(kind,[]):
            if not isinstance(row,dict):
                finding("critical","RECORD_NOT_OBJECT",f"{paths[n]}:{kind}")
                continue
            all_records[kind].append((n,row))
for kind,field in collections_by_kind.items():
    vals=[]
    for n,row in all_records[kind]:
        value=row.get(field) or row.get("key") or row.get("id")
        if not value and kind=="events":
            # Production WorldEventModel has title, due_day and due_at,
            # not an event_key. This compound locator detects collisions.
            value=f"{n}|{row.get('title')}|{row.get('due_day')}"
        if not value:
            finding("warning","MISSING_RECORD_ID",f"{paths[n]}:{kind}:{row.get('title',row.get('name'))}")
        else: vals.append((value,n))
    count=collections.Counter(x for x,n in vals)
    dup={k:[n for v,n in vals if v==k] for k,v in count.items() if v>1}
    if dup:
        if kind=="npcs":
            # The same companion is meant to return with the same stable ID.
            for identity,numbers in dup.items():
                rows=[r for num,r in all_records[kind] if r.get(field)==identity]
                if len({r.get("name") for r in rows})>1:
                    finding("critical","CONFLICTING_NPC_IDENTITY",f"{identity} changes name",chapters=numbers)
            metrics["reused_canonical_npc_identities"]=len(dup)
        else:
            finding("critical","DUPLICATE_GLOBAL_RECORD_ID",f"{kind} has repeated identities",records=dup)
    metrics[kind+"_count"]=len(all_records[kind])
    metrics[kind+"_unique_ids"]=len(count)
keys={row.get("quest_key") for _,row in all_records["quests"] if row.get("quest_key")}
missing=[]
graph={}
for chapter,row in all_records["quests"]:
    qid=row.get("quest_key")
    if not qid: continue
    pre=row.get("prerequisites",[])
    if not isinstance(pre,list):
        finding("critical","QUEST_PREREQUISITES_INVALID",qid); continue
    graph[qid]=pre
    for p in pre:
        if p not in keys: missing.append({"quest":qid,"prerequisite":p,"chapter":chapter})
    objectives=row.get("objectives",[])
    if not isinstance(objectives,list) or len(objectives)==0:
        finding("critical","QUEST_HAS_NO_OBJECTIVES",qid)
    else:
        ids=[o.get("id") for o in objectives if isinstance(o,dict)]
        if len(ids)!=len(set(ids)):finding("critical","DUPLICATE_QUEST_OBJECTIVE",qid)
if missing:finding("critical","DANGLING_QUEST_PREREQUISITE","Unresolvable prerequisites",edges=missing)
white,grey,black=set(),set(),set()
def dfs(node,stack):
    if node in grey:
        finding("critical","QUEST_CYCLE","Circular quest dependency",cycle=stack+[node]);return
    if node in black:return
    grey.add(node)
    for nxt in graph.get(node,[]):
        if nxt in graph:dfs(nxt,stack+[node])
    grey.discard(node);black.add(node)
for k in graph:dfs(k,[])
metrics["quest_dependency_count"]=sum(len(v) for v in graph.values())
metrics["quest_dangling"]=len(missing)
metrics["quest_dependency_cycles"]=len([x for x in issues if x["code"]=="QUEST_CYCLE"])
# All ten sequential adventure source chapters have concrete interactive substance.
for n in plot:
    if n not in chapters:continue
    d=chapters[n]
    for field in ("quests","scenes","npcs","hazards","pois"):
        require(len(d.get(field,[]))>0,"PLOT_CHAPTER_EMPTY_CONTENT",f"Chapter {n}: {field}")
# Source-bound chronology: distinguish chapter import from actual UI journey.
metrics["plot_chain_asset_ready"]=all(i in chapters for i in plot) and not any(x["severity"]=="critical" for x in issues)
metrics["campaign_runtime_playthrough_executed"]=False
metrics["android_appdb_tested"]=False
# Endurance: real asset quest/objective state saved in SQLite file and reopened,
# with 20 passes to detect accidental identity/resource leaks in the fixture.
fd,dbpath=tempfile.mkstemp(prefix="pf2-r35-deep-",suffix=".sqlite")
os.close(fd)
try:
    con=sqlite3.connect(dbpath)
    con.execute("PRAGMA journal_mode=WAL")
    con.executescript("""
    CREATE TABLE audit_quests(cycle INTEGER,quest_key TEXT,chapter INTEGER,status TEXT,PRIMARY KEY(cycle,quest_key));
    CREATE TABLE audit_objectives(cycle INTEGER,quest_key TEXT,objective_id TEXT,completed INTEGER,PRIMARY KEY(cycle,quest_key,objective_id));
    CREATE TABLE audit_checkpoint(cycle INTEGER PRIMARY KEY,epoch INTEGER,hash TEXT);
    """)
    expected_rows=0
    for cycle in range(20):
        with con:
            for ch,row in all_records["quests"]:
                q=row.get("quest_key")
                if not q:continue
                con.execute("INSERT INTO audit_quests VALUES(?,?,?,?)",(cycle,q,ch,"completed"))
                for o in row.get("objectives",[]):
                    if not isinstance(o,dict) or not o.get("id"):continue
                    con.execute("INSERT INTO audit_objectives VALUES(?,?,?,?)",(cycle,q,o["id"],1))
                    expected_rows+=1
            con.execute("INSERT INTO audit_checkpoint VALUES(?,?,?)",(cycle,cycle*31,hashlib.sha256(str(cycle).encode()).hexdigest()))
        con.close()
        con=sqlite3.connect(dbpath)
        assert con.execute("SELECT COUNT(*) FROM audit_checkpoint").fetchone()[0]==cycle+1
        assert con.execute("SELECT COUNT(*) FROM audit_objectives").fetchone()[0]==expected_rows
    before=con.execute("SELECT COUNT(*) FROM audit_quests").fetchone()[0]
    try:
        with con:
            con.execute("INSERT INTO audit_quests VALUES(?,?,?,?)",(999999,"intentionally_rollback",1,"pending"))
            raise ValueError("intentional transaction rollback")
    except ValueError:pass
    assert con.execute("SELECT COUNT(*) FROM audit_quests").fetchone()[0]==before
    con.close()
    metrics["asset_fixture_persistence"]={"cycles":20,"objectives_persisted":expected_rows,"rollback":"pass","sqlite_reopen":"pass","classification":"simulated fixture; NOT production Android AppDb"}
except Exception as e:
    finding("critical","SQLITE_ASSET_STRESS_FAILED",str(e),trace=traceback.format_exc()[-1500:])
finally:
    try:con.close()
    except:pass
    for ext in ("","-shm","-wal"):
        try:os.unlink(dbpath+ext)
        except FileNotFoundError:pass
# Limitations are non-negotiable certification gates:
finding("evidence_gap","OFFICIAL_PDFS_LOCATED_SEMANTIC_COMPARISON_PENDING","Official Kingmaker.pdf (Drive ID 1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3), Companion Guide, and PF2e Core have been identified; this automated catalog audit still does not prove page-by-page correspondence or playable branches.")
finding("evidence_gap","NO_ANDROID_REAL_PLAYTHROUGH","Catalog imports and SQLite fixture exercise do not execute a full user-directed campaign through Android UI / Gemini / live rules engine.")
finding("evidence_gap","NO_PRODUCTION_APPDB_ENDURANCE","The SQLite endurance replay uses actual quest content but does not call the Android AppDb backup/restore on a running device.")
finding("evidence_gap","NO_REAL_TURN_WAR_SAVE_SEQUENCE","Existing JVM kingdom/warfare mechanics tests are separate; no uninterrupted real-device kingdom + war + long-session restore was performed.")
result={"schema":"braseiro-pf2-r35-deep-certification-v1","base_release":"PATHFINDER2E-9.0.36-R35","generated_utc":time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime()),"chapters":paths,"metrics":metrics,"findings":issues,"counts":dict(collections.Counter(x["severity"] for x in issues))}
(out/"REPORT_DATA.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
md=["# Auditoria aprofundada R35 — Kingmaker","",f"Base: R35 (9.0.36); Fonte: **arquivos reais da distribuição**.","","## Resultados comprovados"]
for k,v in metrics.items(): md.append(f"- **{k}:** {json.dumps(v,ensure_ascii=False)[:1200]}")
md+=["","## Achados e pendências por evidência"]
for x in issues:md.append(f"- **{x['severity'].upper()} — {x['code']}**: {x['description']}")
md+=["","## Delimitação de homologação","- A execução do fixture SQLite NÃO certifica o AppDb Android.","- O catálogo de capítulos NÃO certifica progressão real do usuário no VTT.","- A ausência dos PDFs NÃO pode virar aprovação da equivalência com os livros.","- Testes de guerra/reino anteriores NÃO substituem uma partida interativa de ponta a ponta.","- Os arquivos 11 e 12 são compêndios, não os últimos dois capítulos lineares da campanha."]
(out/"AUDITORIA_R35.md").write_text("\n".join(md)+"\n",encoding="utf-8")
print("AUDIT_RESULT="+json.dumps({"metrics":{k:v for k,v in metrics.items() if not k.endswith("source_identity")},"counts":result["counts"]},ensure_ascii=False))
for issue in issues:
    print("AUDIT_FINDING="+issue["severity"]+" "+issue["code"]+" "+issue["description"][:200])
raise SystemExit(2 if any(x["severity"]=="critical" for x in issues) else 0)
