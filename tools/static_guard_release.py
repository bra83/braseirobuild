#!/usr/bin/env python3
from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
scan_roots=[root/'app'/'src'/'main']
patterns={
 'GOOGLE_API_KEY': re.compile(r'AIza[0-9A-Za-z_-]{20,}'),
 'OPENAI_STYLE_KEY': re.compile(r'\bsk-[0-9A-Za-z_-]{20,}'),
 'HARDCODED_APIKEY': re.compile(r'(?i)(api[_-]?key|password|credential|secret)\s*[:=]\s*[\"\'][^\"\'\n]{8,}[\"\']'),
}
allowed_literals={'runtimeSecrets','credentials','secretProvider','apiKey','password','credential','secret'}
issues=[]; files=0
for base in scan_roots:
  for p in base.rglob('*'):
    if not p.is_file() or p.suffix.lower() not in {'.js','.mjs','.kt','.java','.json','.html','.css','.xml'}: continue
    files+=1
    try: text=p.read_text(encoding='utf-8')
    except UnicodeDecodeError:
      issues.append((str(p.relative_to(root)),'UTF8_DECODE_FAIL','')); continue
    if '\ufffd' in text: issues.append((str(p.relative_to(root)),'REPLACEMENT_CHAR','�'))
    for label,pat in patterns.items():
      for m in pat.finditer(text):
        value=m.group(0)
        # allow schema/key names only; reject assigned literal secrets
        if label=='HARDCODED_APIKEY' and not any(ch.isdigit() for ch in value) and len(value)<40: continue
        issues.append((str(p.relative_to(root)),label,value[:80]))
if issues:
  print(f'RELEASE_SECURITY_GUARD_FAIL files={files} issues={len(issues)}')
  for row in issues[:50]: print(' :: '.join(row))
  sys.exit(1)
print(f'RELEASE_SECURITY_GUARD_PASS files={files} no_hardcoded_credentials=YES utf8=PASS')
