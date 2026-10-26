from pathlib import Path
import re, sys, json
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java'
res=root/'app/src/main/res'
screens=java/'com/braseiro/talesvtt/screens'
expected={
'SceneScreen.kt','TroubleScreen.kt','KidScreen.kt','GroupScreen.kt','MysteryScreen.kt','MapScreen.kt',
'BackpackScreen.kt','MenuScreen.kt','ConnectionsScreen.kt','LocationScreen.kt','EverydayScreen.kt','ClimaxScreen.kt','EndingScreen.kt'
}
actual={p.name for p in screens.glob('*.kt')}
texts='\n'.join(p.read_text(errors='ignore') for p in java.rglob('*.kt'))
ui_texts='\n'.join(p.read_text(errors='ignore') for p in list(screens.glob('*.kt')) + list((java/'com/braseiro/talesvtt/components').glob('*.kt')))
forbidden={
'question_marks': r'\?\?\?',
'tactical_radar': r'RADAR T[ÁA]TICO',
'hp': r'\bHP\b|PONTOS DE VIDA',
'initiative': r'INICIATIVA',
'encumbrance': r'ENCUMBRANCE|CAPACIDADE\s*[:=]',
'demo_triggers': r'startDemoExtendedTrouble|requestGateInvestigation|XP calculado nesta sessão: \+4',
}
forbidden_hits={k:len(re.findall(v, texts if k=='demo_triggers' else ui_texts, re.I)) for k,v in forbidden.items()}
placeholder_hits=[]
for p in screens.glob('*.kt'):
    t=p.read_text()
    if 'PlaceholderPanel' in t or 'TestPanelPlaceholder' in t:
        placeholder_hits.append(p.name)
refs=set(re.findall(r'R\.drawable\.([A-Za-z0-9_]+)',texts))
drawables={p.stem for p in res.rglob('*') if p.is_file() and 'drawable' in str(p.parent)}
missing=sorted(refs-drawables)
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(errors='ignore')
old_package_hits=len(re.findall(r'com\.example|com\.aistudio\.talesvtt', texts + '\n' + (root/'app/build.gradle.kts').read_text(errors='ignore')))
backup_disabled='android:allowBackup="false"' in manifest
required_engine_tokens={
  'extended_begin':'BeginExtendedKidScene',
  'extended_finalize':'FinalizeExtendedKidSceneFromTest',
  'ending_modes':'EndingMode',
  'birthday_review':'SetBirthdayPassed',
  'birthday_attribute':'ApplyBirthdayAttribute',
  'next_session':'StartNextSession',
}
engine_file=(java/'com/braseiro/talesvtt/game/TalesCore.kt').read_text(errors='ignore')
missing_engine_tokens=[name for name,token in required_engine_tokens.items() if token not in engine_file]
scaffold_files=[str(x.relative_to(root)) for x in [
  java/'com/braseiro/talesvtt/components/PlaceholderPanel.kt',
  java/'com/braseiro/talesvtt/components/TestPanelPlaceholder.kt'
] if x.exists()]
# crude brace sanity ignores strings but catches major truncation
brace_bad=[]
for p in java.rglob('*.kt'):
    t=p.read_text()
    if t.count('{') != t.count('}'):
        brace_bad.append(p.relative_to(root).as_posix())
report={
'screens_expected':len(expected),'screens_present':len(expected & actual),'missing_screens':sorted(expected-actual),
'drawable_refs':len(refs),'drawable_files':len(drawables),'missing_drawables':missing,
'forbidden_hits':forbidden_hits,'placeholder_screen_hits':placeholder_hits,'brace_bad':brace_bad,
'old_package_hits':old_package_hits,'backup_disabled':backup_disabled,'missing_engine_tokens':missing_engine_tokens,'scaffold_files':scaffold_files,
}
report['pass']=not report['missing_screens'] and not missing and not any(forbidden_hits.values()) and not placeholder_hits and not brace_bad and old_package_hits==0 and backup_disabled and not missing_engine_tokens and not scaffold_files
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(0 if report['pass'] else 1)
