from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
prod = list((ROOT/'app/src/main/assets/www').rglob('*.js')) + list((ROOT/'app/src/main/java').rglob('*.kt'))
violations=[]
for p in prod:
    text=p.read_text(errors='replace')
    if 'Math.random(' in text:
        violations.append((p,'Math.random mechanical/procedural randomness forbidden'))
    if p.suffix=='.js' and ('localStorage.' in text or 'localStorage[' in text):
        violations.append((p,'localStorage cannot be canonical campaign state'))
    if p.name!='BraseiroBridge.kt' and '@JavascriptInterface' in text:
        violations.append((p,'unexpected JS/native bridge surface'))
if violations:
    for p,msg in violations: print('FAIL',p.relative_to(ROOT),msg)
    raise SystemExit(1)
print(f'STATIC_GUARD_PASS files={len(prod)} no=Math.random/localStorage/extra-bridge')
