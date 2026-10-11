#!/usr/bin/env python3
"""Official multi-skill structures: Castle/Town Hall. Apply after 16-entry patch.
Fail closed: exact official 4-way skill list + rank, known PDF and unique Java anchors.
"""
from pathlib import Path
import json,re,sys
REL=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRulesEngine.java")
MANIFEST=Path(__file__).resolve().parents[1]/"pf2-kingmaker-certification/kingmaker_official_structures_p544_552.json"
MULTI={"castle":"CASTLE","town_hall":"TOWN HALL"}
FIELDS=("level","lots","rp","lumber","stone","ore","luxuries")

def transform(source,manifest):
    if manifest.get("source_drive_file_id")!="1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3" or len(manifest.get("structures",[]))!=73:
        raise RuntimeError("Incorrect official PDF manifest")
    records={entry["official_name"]:entry for entry in manifest["structures"]}
    head="public JSONObject structure(String key)"
    if source.count(head)!=1:raise RuntimeError("Source catalog not uniquely identified")
    a=source.index(head)
    b=source.find("\n    public ",a+len(head))
    if b<0:raise RuntimeError("Catalog method end missing")
    method=source[a:b]
    for key,name in MULTI.items():
        entry=records[name]
        raw=entry["construction"]
        matches=re.findall(r'([A-Za-z]+) \((trained|expert|master|legendary)\)',raw)
        if [x[0].lower() for x in matches]!=["defense","industry","magic","statecraft"] or len(set(x[1] for x in matches))!=1:
            raise RuntimeError("Official multi-skill construction changed: "+name)
        requiredRank=matches[0][1]
        marker='if("'+key+'".equals(k)){'
        if method.count(marker)!=1:raise RuntimeError("Structure anchor drift: "+key)
        mstart=method.index(marker)
        nextBranch=re.search(r'}\s*else\s+if\s*\(',method[mstart+len(marker):])
        if nextBranch is None:raise RuntimeError("Next branch missing: "+key)
        stop=mstart+len(marker)+nextBranch.start()
        block=method[mstart:stop]
        values={"level":entry["official_level"],"lots":entry["lots"],**entry["costs"]}
        for field in FIELDS:
            pattern=r'(\.put\("'+field+r'",\s*)\d+(\))'
            count=len(re.findall(pattern,block))
            if count!=1:raise RuntimeError(key+" "+field+" drift "+str(count))
            block=re.sub(pattern,lambda m:m.group(1)+str(values[field])+m.group(2),block,count=1)
        if "construction_skills" in block:raise RuntimeError("Already patched: "+key)
        ins='.put("construction_skills","defense,industry,magic,statecraft").put("construction_rank","'+requiredRank+'").put("construction_dc",'+str(entry["construction_dc"])+')'
        anchor='.put("residential_lots",'
        if block.count(anchor)!=1:raise RuntimeError("Insertion anchor drift: "+key)
        block=block.replace(anchor,ins+anchor,1)
        method=method[:mstart]+block+method[stop:]
    return source[:a]+method+source[b:]

def apply(root):
    root=Path(root).resolve()
    if not (root/"version.properties").is_file() or "VERSION_NAME=9.0.37" not in (root/"version.properties").read_text(encoding="utf-8"):
        raise RuntimeError("Expected verified R36 source")
    manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
    p=root/REL
    before=p.read_text(encoding="utf-8")
    after=transform(before,manifest)
    p.write_text(after,encoding="utf-8")
    return p

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_multi_structure.py <R36_project>")
    print("R36_OFFICIAL_MULTISKILL_CASTLE_TOWN_HALL_PATCHED",apply(sys.argv[1]))
