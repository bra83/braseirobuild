#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path(r"C:\actions-runner\_work\_temp\sw-r54-p27\work\android")
vtt=(root/'app/src/main/assets/vtt') if (root/'app/src/main/assets/vtt').is_dir() else (root/'assets/vtt')
targets=[
 vtt/'js/core/controller.mjs',
 vtt/'js/core/uiBridge.bundle.js',
 vtt/'js/rules/eoteCharacterCreation.mjs',
 vtt/'js/rules/characterCreation.mjs',
 vtt/'js/rules/fringeGroup.mjs',
 vtt/'js/model/character.mjs',
 vtt/'r46/r46-shell.bundle.js',
 vtt/'r46/final-runtime.js',
]
terms=['campaignModeSummary','setCampaignMode','creationPreview','createCustomCharacter','createAorCoreCharacter','createEoteCoreCharacter','obligationBenefits','additionalObligation','obligations','duties','dutyType','dutyReductions','Duty','Contribution','MIXED','obligationType','addDutyEntry','addObligationEntry','validateCharacter']
for p in targets:
    print("\n###FILE",p)
    if not p.is_file():
        print("MISSING"); continue
    s=p.read_text(encoding='utf-8',errors='replace')
    print("SIZE",len(s))
    for term in terms:
        hits=[]; start=0
        while True:
            i=s.find(term,start)
            if i<0: break
            hits.append(i); start=i+len(term)
            if len(hits)>=4: break
        if not hits: continue
        print(f"\n--TERM {term} HITS {hits}")
        for i in hits[:2]:
            a=max(0,i-1200); b=min(len(s),i+4200)
            print(s[a:b])
            print("\n--SNIP-END--")
