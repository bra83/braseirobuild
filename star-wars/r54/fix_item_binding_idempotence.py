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
old = r'''  const exact=idx.get(r54Norm(name));if(!exact)continue;
  host.querySelectorAll('.r53-item-visual,.r54-item-visual').forEach(x=>x.remove());const fig=document.createElement('figure');fig.className='r54-item-visual';fig.innerHTML=`<img loading=\"lazy\" alt=\"${name.replace(/[\"<&]/g,'')}\" src=\"${exact}\">`;host.insertBefore(fig,host.firstChild);
'''
new = r'''  const exact=idx.get(r54Norm(name));if(!exact)continue;
  const key=r54Norm(name),current=host.querySelector(':scope > .r54-item-visual');
  if(current){const img=current.querySelector('img');if(current.dataset.r54VisualBound===key&&img?.getAttribute('src')===exact)continue;current.remove()}
  host.querySelectorAll(':scope > .r53-item-visual').forEach(x=>x.remove());
  const fig=document.createElement('figure');fig.className='r54-item-visual';fig.dataset.r54VisualBound=key;fig.innerHTML=`<img loading=\"lazy\" alt=\"${name.replace(/[\"<&]/g,'')}\" src=\"${exact}\">`;host.insertBefore(fig,host.firstChild);
'''

count = text.count(old)
if count != 1:
    raise SystemExit(f'expected exactly one legacy R54 item binding block, got {count}')

text = text.replace(old, new, 1)
runtime.write_text(text, encoding='utf-8')

check = runtime.read_text(encoding='utf-8')
assert 'dataset.r54VisualBound=key' in check
assert "host.querySelectorAll('.r53-item-visual,.r54-item-visual').forEach(x=>x.remove())" not in check
assert "current.dataset.r54VisualBound===key" in check
print('R54 ITEM BINDING IDEMPOTENCE PASS')
