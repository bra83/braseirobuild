#!/usr/bin/env python3
"""Read-only source inspection for Build Structure and Claim Hex UI on R36."""
import re
import sys
from pathlib import Path

root=Path(sys.argv[1]).resolve()
for rel in [
    "android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java",
    "android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java",
]:
    p=root/rel
    lines=p.read_text(encoding="utf-8").splitlines()
    print("=== INSPECTION:",rel,"===")
    terms=['buildStructure','resolveBuildStructure','claimHex(','Build Structure','Rubble','incomplete','obra inacabada','repairStructure','public JSONObject recordCheck(']
    hits=[i for i,line in enumerate(lines) if any(term.lower() in line.lower() for term in terms)]
    if p.name=="KingmakerRepository.java":
        print("=== FULL SINGLE-LINE CONTRACTS ===")
        for num in (241,244,254,280):
            print(f"{num}: {lines[num-1]}")
    print("MATCHES:",len(hits))
    emitted=set()
    for i in hits[:55]:
        lo=max(0,i-5)
        hi=min(len(lines),i+(135 if 'public JSONObject buildStructure(' in lines[i] or 'public JSONObject recordCheck(' in lines[i] else 85 if 'public JSONObject addStructure(' in lines[i] or 'public JSONObject repairStructure(' in lines[i] else 55 if 'public JSONObject resolveBuildStructure(' in lines[i] else 25))
        for n in range(lo,hi):
            if n not in emitted:
                print(f"{n+1}: {lines[n][:400]}")
                emitted.add(n)
        print("---")
    if not hits:print("NO_MATCH")

# Production UI inspection for R36 Android navigation failures. Read-only.
# The app is reconstructed from the pinned R35 ZIP by the CI workflow.
ui=root/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java"
ui_lines=ui.read_text(encoding="utf-8").splitlines()
needles=("INICIAR TURNO QUANDO VOLTAR AO REINO","INICIAR NOVO TURNO",
         "ABRIR ADMINISTRAÇÃO AVANÇADA","CONFIGURAÇÃO AVANÇADA DO REINO",
         "EXÉRCITOS E BATALHAS","COMEÇAR CAMPANHA","KINGMAKER SOLO",
         "showKingmaker","renderKingmaker","KingmakerSection",
         "showKingdom","kingmakerAdmin","kingmakerSolo")
matches=[i for i,line in enumerate(ui_lines) if any(x.casefold() in line.casefold() for x in needles)]
print("=== PF2 R36 ACTUAL ANDROID NAVIGATION/UI DIAGNOSTIC ===",flush=True)
print("matching lines:",len(matches),"of",len(ui_lines),flush=True)
printed=set()
for i in matches[:90]:
    for j in range(max(0,i-3),min(len(ui_lines),i+6)):
        if j not in printed:
            print(f"PF2_UI_SOURCE {j+1}: {ui_lines[j][:2200]}",flush=True)
            printed.add(j)
    print("---- PF2 UI match ----",flush=True)
