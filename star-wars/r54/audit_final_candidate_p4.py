#!/usr/bin/env python3
from pathlib import Path
import sys,re
if len(sys.argv)!=2: raise SystemExit('usage: audit_final_candidate_p4.py <android-root>')
r=Path(sys.argv[1]).resolve(); v=r/'app/src/main/assets/vtt'; j=r/'app/src/main/java/com/braseiro/starwarsedge'
need=[j/'MainActivity.java',j/'GeminiBridge.java']
for p in need:
 if not p.is_file(): raise SystemExit(f'MISSING {p}')
M=(j/'MainActivity.java').read_text('utf-8'); G=(j/'GeminiBridge.java').read_text('utf-8')
def gate(name,text,tokens):
 miss=[x for x in tokens if x not in text]
 if miss: raise SystemExit(f'FAIL {name}: {miss}')
 print('PASS',name)
gate('PDF_PERSIST_INDEX',M,['takePersistableUriPermission','FLAG_GRANT_PERSISTABLE_URI_PERMISSION','indexPdf(','pages.jsonl','searchDocumentsJson','INDEXADO'])
gate('GEMINI_PERSIST',G,['AndroidKeyStore','SharedPreferences','keyPreferences','readApiKey()'])
gate('MODEL_PERSIST_SEPARATE',G,['modelPreferences'])
gate('BARBARA_LONG',G,['900-1400 palavras','8-14 parágrafos','nunca reduza a narração a uma frase de fallback'])
alltxt='\n'.join(p.read_text('utf-8',errors='ignore') for p in v.rglob('*') if p.is_file() and p.suffix.lower() in {'.js','.html','.css','.json'})
gate('MAP_INTERACTION',alltxt,['local.map.galaxy'])
for token in ['zoom','pan']:
 if token.lower() not in alltxt.lower(): raise SystemExit('FAIL MAP_'+token.upper())
print('PASS MAP_ZOOM_PAN')
for token in ['NPC','facção','droide','rota','viagem','nave','AoR']:
 if token.lower() not in alltxt.lower(): raise SystemExit('FAIL WORLD '+token)
print('PASS LIVING_WORLD_SURFACES')
if 'inventory.png' in alltxt:
 # UI nav icon is legitimate; reject known item-image fallback patterns only.
 bad=[ln for ln in alltxt.splitlines() if 'inventory.png' in ln and ('fallback' in ln.lower() or 'item' in ln.lower() or 'acquisition' in ln.lower())]
 if bad: raise SystemExit('FAIL ITEM_PLACEHOLDER inventory.png used as item fallback')
print('PASS NO_ITEM_PLACEHOLDER_FALLBACK')
imgs=[p for p in v.rglob('*') if p.is_file() and p.suffix.lower() in {'.png','.jpg','.jpeg','.webp'}]
if not imgs: raise SystemExit('FAIL ASSETS no images')
print('PASS ASSET_FILES',len(imgs))
print('R54_P4_FINAL_CANDIDATE_STATIC_PASS')
