#!/usr/bin/env python3
from pathlib import Path
import json,sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path.cwd()
errors=[]
rt=list(root.rglob("final-runtime.js"))
if not rt: errors.append("final-runtime.js ausente")
else:
 s=max(rt,key=lambda p:p.stat().st_size).read_text(encoding="utf-8",errors="replace")
 for needle in ["MIXED","Obrigação e Dever","__BRASEIRO_CAMPAIGN_UI__","contribution:mode!=='EOTE'"]:
  if needle not in s: errors.append("runtime sem "+needle)
m=Path(__file__).with_name("under_a_black_sun.pt-BR.json")
data=json.loads(m.read_text(encoding="utf-8"))
for k in ["id","title","source","rulesProfile","campaignCompatibility","entityBindings"]:
 if not data.get(k): errors.append("manifest sem "+k)
if "MIXED" not in data["campaignCompatibility"]: errors.append("aventura sem compatibilidade MIXED")
if errors:
 print("FAIL"); [print("- "+x) for x in errors]; raise SystemExit(1)
print("PASS: gate P27 campanha+aventura")
