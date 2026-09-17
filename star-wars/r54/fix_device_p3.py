#!/usr/bin/env python3
from pathlib import Path
import sys
if len(sys.argv)!=2: raise SystemExit('usage: fix_device_p3.py <android-project-root>')
root=Path(sys.argv[1]).resolve()
g=root/'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'
m=root/'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'
for p in (g,m):
    if not p.is_file(): raise SystemExit(f'missing {p}')
s=g.read_text('utf-8')
old='Produza 400-700 palavras em 4-7 parágrafos, mantendo foco na situação presente e espaço real para decisão do jogador.'
new='Produza 900-1400 palavras em 8-14 parágrafos quando estiver iniciando uma aventura ou narrando uma cena principal; em transições menores, nunca reduza a narração a uma frase de fallback. Mantenha foco na situação presente, continuidade com o mundo persistente e espaço real para decisão do jogador.'
if old in s: s=s.replace(old,new,1)
elif new not in s: raise SystemExit('Barbara long-narration contract anchor missing')
g.write_text(s,'utf-8')
# Hard gates for persistence/indexing already implemented in the Android host.
ms=m.read_text('utf-8')
required=['takePersistableUriPermission','FLAG_GRANT_PERSISTABLE_URI_PERMISSION','private void indexPdf(','pages.jsonl','"INDEXADO"','searchDocumentsJson']
missing=[x for x in required if x not in ms]
if missing: raise SystemExit('PDF persistence/indexing contract incomplete: '+repr(missing))
# Gemini persistence/model separation must be device-persistent, not transient JS-only state.
gs=g.read_text('utf-8')
required_g=['AndroidKeyStore','SharedPreferences','modelPreferences','keyPreferences','readApiKey()']
missing=[x for x in required_g if x not in gs]
if missing: raise SystemExit('Gemini persistence contract incomplete: '+repr(missing))
print('R54_DEVICE_P3_PATCH_PASS barbara_words=900-1400 pdf_persistent=1 gemini_persistent=1')
