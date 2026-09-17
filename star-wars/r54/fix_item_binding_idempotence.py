#!/usr/bin/env python3
from __future__ import annotations
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_item_binding_idempotence.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
runtime = root / 'app/src/main/assets/vtt/r46/final-runtime.js'
if not runtime.is_file():
    raise SystemExit(f'missing runtime: {runtime}')

text = runtime.read_text(encoding='utf-8')

legacy = '''  const exact=idx.get(r54Norm(name));if(!exact)continue;
  host.querySelectorAll('.r53-item-visual,.r54-item-visual').forEach(x=>x.remove());const fig=document.createElement('figure');fig.className='r54-item-visual';fig.innerHTML=`<img loading="lazy" alt="${name.replace(/["<&]/g,'')}" src="${exact}">`;host.insertBefore(fig,host.firstChild);
'''

partial = '''  const exact=idx.get(r54Norm(name));if(!exact)continue;
  const current=host.querySelector('.r54-item-visual img');if(current&&current.getAttribute('src')===exact)continue;
  host.querySelectorAll('.r53-item-visual,.r54-item-visual').forEach(x=>x.remove());const fig=document.createElement('figure');fig.className='r54-item-visual';fig.innerHTML=`<img loading="lazy" alt="${name.replace(/["<&]/g,'')}" src="${exact}">`;host.insertBefore(fig,host.firstChild);
'''

final = '''  const exact=idx.get(r54Norm(name));if(!exact)continue;
  const key=r54Norm(name),current=host.querySelector(':scope > .r54-item-visual');
  if(current){const img=current.querySelector('img');if(current.dataset.r54VisualBound===key&&img?.getAttribute('src')===exact)continue;current.remove()}
  host.querySelectorAll(':scope > .r53-item-visual').forEach(x=>x.remove());
  const fig=document.createElement('figure');fig.className='r54-item-visual';fig.dataset.r54VisualBound=key;fig.innerHTML=`<img loading="lazy" alt="${name.replace(/["<&]/g,'')}" src="${exact}">`;host.insertBefore(fig,host.firstChild);
'''

final_marker = 'fig.dataset.r54VisualBound=key'
legacy_count = text.count(legacy)
partial_count = text.count(partial)
final_count = text.count(final_marker)

if final_count == 1 and legacy_count == 0 and partial_count == 0:
    # Already repaired: intentionally no-op so reruns/build retries are safe.
    pass
elif final_count == 0 and legacy_count + partial_count == 1:
    source = partial if partial_count == 1 else legacy
    text = text.replace(source, final, 1)
    runtime.write_text(text, encoding='utf-8')
else:
    raise SystemExit(
        'unexpected R54 item binding state: '
        f'legacy={legacy_count}, partial={partial_count}, final={final_count}'
    )

check = runtime.read_text(encoding='utf-8')
assert check.count(final_marker) == 1
assert "host.querySelectorAll('.r53-item-visual,.r54-item-visual').forEach(x=>x.remove())" not in check
assert "current.dataset.r54VisualBound===key" in check
assert "host.querySelectorAll(':scope > .r53-item-visual').forEach(x=>x.remove())" in check
print('R54 ITEM BINDING IDEMPOTENCE PASS')
