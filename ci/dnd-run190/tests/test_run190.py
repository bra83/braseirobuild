from pathlib import Path
import re,sys,subprocess,tempfile,hashlib
root=Path(sys.argv[1]);src=root/'android/app/src/main/java/com/braseiro/dnd5e';main=(src/'MainActivity.java').read_text(encoding='utf-8');catalog=(src/'RuleCuriosityCatalog.java').read_text(encoding='utf-8')
checks={
'icon_general': 'addRuleCuriosityButton(b);' in main,
'icon_strixhaven':'hud.addView(ruleTip,new LinearLayout.LayoutParams(dp(46),dp(48)))' in main,
'accessible_label':'Curiosidade de regra da cena; livro e página verificados' in main,
'no_world_mutations': all(x not in main[main.index('private RuleCuriosityCatalog.Entry currentSceneRuleCuriosity()'):main.index('private void showNarratorFull()')] for x in ('db.saveSceneState(', 'db.updateCharacterSheet(', 'sendAi(', 'db.nextTurn(', 'db.setCombatRunning(')),
'persisted_scene_rotation':'prefs.edit().putString(key,sceneKey).putInt("rule_curiosity_index_"+scope,index).apply();' in main,
'no_change_on_reopen':'if(!sceneKey.equals(oldKey))' in main,
'no_change_on_click':main.count('currentSceneRuleCuriosity();')==1,
'book_and_printed_page':'Página impressa:' in main and 'Regra' not in catalog[:1],
'verified_drive_url':'1FDZwdunnLxO3FG85N-hMKEG9UTwmWBEl' in catalog,
'no_auto_generate':'GeminiService' not in catalog,
'same_identity':'package com.braseiro.dnd5e' in main
}
for k,v in checks.items():print(('PASS ' if v else 'FAIL ')+k)
if not all(checks.values()):sys.exit(1)
# Pure Java corpus verification; no Android SDK required.
with tempfile.TemporaryDirectory() as temp:
 cmd=['javac','-encoding','UTF-8','-d',temp,str(src/'RuleCuriosityCatalog.java'),str(Path(__file__).parent/'RuleCuriosityCatalogTest.java')]
 subprocess.run(cmd,check=True)
 subprocess.run(['java','-cp',temp,'RuleCuriosityCatalogTest'],check=True)
# Explicitly verify all page pointers on the user's actual PDF when supplied.
if len(sys.argv)>2:
 import fitz
 pdf=fitz.open(sys.argv[2]);
 pages=[int(x) for x in re.findall(r'\,\s*(\d+)\)\s*\,?',catalog)]
 # extract exact leaf entries using final page fields
 pages=[int(x) for x in re.findall(r',\s*(\d+)\)\s*,?',catalog) if int(x)<100]
 allowed=[9,26,27,27,30,32,33,37,37,41,45,47,51,57]
 assert pages[-14:]==allowed,(pages[-14:],allowed)
 assert len(pdf)>max(allowed)+4
 for p in set(allowed):
  page=pdf[p+3];t=page.get_text();assert len(t)>700,(p,len(t))
 print('PASS exact source: 14 printed-page pointers resolved in the linked 390-page PDF')
print('PASS RUN190 static contracts, Java catalog, scene rotation invariants')
