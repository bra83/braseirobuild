#!/usr/bin/env python3
from pathlib import Path
import json,sys
root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
errors=[]
rt=list(root.rglob("final-runtime.js"))
if not rt:
    errors.append("final-runtime.js ausente")
else:
    s=max(rt,key=lambda p:p.stat().st_size).read_text(encoding="utf-8",errors="replace")
    for needle in ["MIXED","Obrigação e Dever","__BRASEIRO_CAMPAIGN_UI__","contribution:mode!=='EOTE'","R54_P27_UNDER_BLACK_SUN","under-a-black-sun","Sob um Sol Negro","Modo de aventura"]:
        if needle not in s: errors.append("runtime sem "+needle)
manifest=json.loads(Path(__file__).with_name("under_a_black_sun.pt-BR.json").read_text(encoding="utf-8"))
for k in ["id","title","source","rulesProfile","campaignCompatibility","entityBindings"]:
    if not manifest.get(k): errors.append("manifest sem "+k)
if "MIXED" not in manifest["campaignCompatibility"]: errors.append("manifest sem compatibilidade MIXED")
pack=json.loads(Path(__file__).with_name("under_a_black_sun_adventure.pt-BR.json").read_text(encoding="utf-8"))
if pack.get("id")!="under-a-black-sun": errors.append("data pack com id inválido")
if pack.get("rules",{}).get("profile")!="EOTE": errors.append("data pack não está ancorado em EOTE")
if pack.get("rules",{}).get("aorPureModeAllowed") is not False: errors.append("AoR puro não está bloqueado")
if set(pack.get("rules",{}).get("compatibleCampaignModes",[]))!={"EOTE","MIXED"}: errors.append("compatibilidade EOTE/MIXED incorreta")
if len(pack.get("episodes",[]))!=3: errors.append("aventura não possui 3 episódios estruturados")
ids=[n.get("id") for ep in pack.get("episodes",[]) for n in ep.get("nodes",[])]
for required in ["hot-pursuit","chop-talk","umbra-club","spyder","zelcomm-tower","sector-943","novacore","showdown"]:
    if required not in ids: errors.append("nó canônico ausente: "+required)
installed=root/"app/src/main/assets/vtt/adventures/under-a-black-sun.pt-BR.json"
if not installed.is_file(): errors.append("data pack não instalado nos assets Android")
else:
    live=json.loads(installed.read_text(encoding="utf-8"))
    if live.get("id")!="under-a-black-sun": errors.append("data pack instalado inválido")
if errors:
    print("FAIL")
    for x in errors: print("- "+x)
    raise SystemExit(1)
print("PASS: P27 EOTE/AOR/MIXED + Sob um Sol Negro")
