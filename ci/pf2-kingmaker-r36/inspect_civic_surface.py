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
    terms=['buildStructure','resolveBuildStructure','claimHex(','Build Structure','Rubble','incomplete','obra inacabada','repairStructure']
    hits=[i for i,line in enumerate(lines) if any(term.lower() in line.lower() for term in terms)]
    print("MATCHES:",len(hits))
    emitted=set()
    for i in hits[:55]:
        lo=max(0,i-5)
        hi=min(len(lines),i+25)
        for n in range(lo,hi):
            if n not in emitted:
                print(f"{n+1}: {lines[n][:400]}")
                emitted.add(n)
        print("---")
    if not hits:print("NO_MATCH")
