#!/usr/bin/env python3
"""Kingmaker 2e canonical structure data correction on extracted, verified R35->R36 source.
Strict mechanical-only crosswalk from the official PDF's 73-entry manifest.
Only 16 unambiguous single-skill entries are touched. Multi-skill structures,
alternative commodity payments, placement/upgrade rules remain OPEN.
This never modifies R35 releases, asset files, maps, audio or other games.
"""
from pathlib import Path
import json, re, sys

MANIFEST=Path(__file__).resolve().parents[1]/"pf2-kingmaker-certification/kingmaker_official_structures_p544_552.json"
REL=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRulesEngine.java")
UNAMBIGUOUS={
    "academy":"ACADEMY", "alchemist":"ALCHEMY LABORATORY",
    "bank":"BANK", "barracks":"BARRACKS",
    "brewery":"BREWERY", "cathedral":"CATHEDRAL",
    "embassy":"EMBASSY", "garrison":"GARRISON",
    "granary":"GRANARY", "library":"LIBRARY",
    "monument":"MONUMENT", "pier":"PIER",
    "smithy":"SMITHY", "stables":"STABLE",
    "temple":"TEMPLE", "theater":"THEATER",
}
FIELDS=("level","lots","rp","lumber","stone","ore","luxuries")

def transform(source, manifest):
    if manifest.get("source_drive_file_id")!="1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3" or len(manifest.get("structures",[]))!=73:
        raise RuntimeError("Wrong official Kingmaker manifest")
    lookup={entry["official_name"]:entry for entry in manifest["structures"]}
    header='public JSONObject structure(String key)'
    if source.count(header)!=1:raise RuntimeError("Structure catalogue source drift")
    methodStart=source.index(header)
    methodEnd=source.find("\n    public ",methodStart+len(header))
    if methodEnd<0:raise RuntimeError("Cannot isolate structure catalogue method")
    method=source[methodStart:methodEnd]
    for key,name in UNAMBIGUOUS.items():
        rec=lookup.get(name)
        if rec is None:raise RuntimeError("Missing official entry: "+name)
        expectedRank=re.fullmatch(r'([A-Za-z]+)(?: \((trained|expert|master|legendary)\))?',rec["construction"])
        if expectedRank is None:raise RuntimeError("Not an unambiguous construction skill: "+name)
        if rec["lots"] is None or " or " in rec["cost_text"]:
            raise RuntimeError("Alternative costs/lots require review: "+name)
        marker='if("'+key+'".equals(k)){'
        if method.count(marker)!=1:raise RuntimeError("Missing or repeated runtime structure key: "+key)
        a=method.index(marker)
        tail=method[a+len(marker):]
        boundary=re.search(r'}\s*else\s*(?:if\s*\(|\{)',tail)
        if boundary is None:raise RuntimeError("No source branch boundary after: "+key)
        stop=a+len(marker)+boundary.start()
        block=method[a:stop]
        values={"level":rec["official_level"],"lots":rec["lots"],**rec["costs"]}
        for field in FIELDS:
            pattern=r'(\.put\("'+field+r'",\s*)\d+(\))'
            matches=list(re.finditer(pattern,block))
            if len(matches)!=1:raise RuntimeError(key+" "+field+" anchor drift "+str(len(matches)))
            block=re.sub(pattern,lambda m:m.group(1)+str(values[field])+m.group(2),block,count=1)
        if "construction_dc" in block:raise RuntimeError("Already patched: "+key)
        insertion='.put("construction_skill","'+expectedRank.group(1).lower()+'").put("construction_rank","'+(expectedRank.group(2) or "untrained")+'").put("construction_dc",'+str(rec["construction_dc"])+')'
        anchor='.put("residential_lots",'
        if block.count(anchor)!=1:raise RuntimeError("Missing metadata insertion anchor: "+key)
        block=block.replace(anchor,insertion+anchor,1)
        method=method[:a]+block+method[stop:]
    return source[:methodStart]+method+source[methodEnd:]

def apply(root):
    root=Path(root).resolve()
    v=root/"version.properties"
    if not v.is_file() or "VERSION_NAME=9.0.37" not in v.read_text(encoding="utf-8"):
        raise RuntimeError("R36 source required")
    manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
    path=root/REL
    before=path.read_text(encoding="utf-8")
    after=transform(before,manifest)
    path.write_text(after,encoding="utf-8")
    return path

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_structure_catalog_values.py <R36_project>")
    print("R36_OFFICIAL_STRUCTURES_16_PATCHED",apply(sys.argv[1]))
