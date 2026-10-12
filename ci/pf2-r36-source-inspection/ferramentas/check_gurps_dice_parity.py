from pathlib import Path
import hashlib, json, sys
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
RAW=ROOT/'android/app/src/main/res/raw'
ref=json.loads((ROOT/'ferramentas/GURPS_DICE_REFERENCE_10_6_0.json').read_text(encoding='utf-8'))
errors=[]
for name,want in ref['classes'].items():
    p=SRC/name
    if not p.exists(): errors.append(f'classe ausente: {name}'); continue
    s=p.read_text(encoding='utf-8').replace('package com.braseiro.pathfinder2e;','package __BRASEIRO_DICE_REFERENCE__;')
    got=hashlib.sha256(s.encode()).hexdigest()
    if got!=want: errors.append(f'{name}: {got} != {want}')
for name,want in ref['sounds'].items():
    p=RAW/name
    if not p.exists(): errors.append(f'som ausente: {name}'); continue
    got=hashlib.sha256(p.read_bytes()).hexdigest()
    if got!=want: errors.append(f'{name}: {got} != {want}')
if errors:
    print('GURPS_DICE_PARITY_FAIL')
    for e in errors: print(' -',e)
    sys.exit(1)
print(f"GURPS_DICE_PARITY_PASS classes={len(ref['classes'])} sounds={len(ref['sounds'])} source={ref['source']}")
