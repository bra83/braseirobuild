from pathlib import Path
import re

p=Path('app/src/main/assets/www/core/combat-service.js')
s=p.read_text()
lines=s.splitlines()

# Merge all named imports from the same special-combat module. Repeated patchers
# are expected to be safe even when an older integration has already landed.
special=[];kept=[]
for line in lines:
    m=re.fullmatch(r"import \{ (.+) \} from './wwn-special-combat-core\.js';",line)
    if m:
        for name in m.group(1).split(','):
            n=name.strip()
            if n and n not in special:special.append(n)
        continue
    kept.append(line)
if special:
    marker="import { resolveSkillCheck } from './wwn-rules-core.js';"
    try:i=kept.index(marker)+1
    except ValueError:raise SystemExit('rules-core import marker missing')
    kept.insert(i,"import { "+', '.join(special)+" } from './wwn-special-combat-core.js';")

# Exact duplicate imports from any other module are never meaningful in ESM.
out=[];seen_imports=set()
for line in kept:
    if line.startswith('import '):
        if line in seen_imports:continue
        seen_imports.add(line)
    out.append(line)
p.write_text('\n'.join(out)+'\n')
print('WWN combat imports normalized')
