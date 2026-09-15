from pathlib import Path
import sys,re
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/braseiro/wwn/dice'
web=root/'app/src/main/assets/www'
main=(root/'app/src/main/java/com/braseiro/wwn/MainActivity.kt').read_text('utf-8')
overlay=(java/'WwnDice3DOverlay.java').read_text('utf-8')
spec=(java/'DiceRollSpec.java').read_text('utf-8')
engine=(java/'PhysicalDiceEngine.java').read_text('utf-8')
action=(web/'core/action-service.js').read_text('utf-8')
rules=(web/'core/wwn-rules-core.js').read_text('utf-8')
scr=(web/'scr016.js').read_text('utf-8')
client=(web/'core/physical-dice-client.js').read_text('utf-8')
checks=[]
def ck(n,c): checks.append((n,bool(c)))
for name in ['DiceRollSpec.java','PhysicalDiceEngine.java','Dice3DView.java','DiceAppearance.java','WwnDice3DOverlay.java']:
    ck('file_'+name,(java/name).is_file())
ck('engine_fixed_120hz','FIXED_DT=1.0/120.0' in engine)
ck('engine_top_face_authority','physicalFaceValue()' in engine and 'rollsFromPhysicalFaces' in engine)
ck('engine_no_preselected_result','randomRolls(' not in engine)
ck('spec_polyhedra',all(x in spec for x in ['t.sides==4','t.sides==6','t.sides==8','t.sides==10','t.sides==12','t.sides==20','t.sides==100']))
ck('native_js_bridge','addJavascriptInterface(PhysicalDiceJavascriptBridge(), "WwnDiceNative")' in main and 'fun startPhysicalDice' in main)
ck('native_provider_proof','ANDROID_NATIVE_PHYSICS' in main and 'physical-dice-top-face-v1' in main)
ck('web_promise_bridge','braseiro-physical-dice' in client and 'rollPhysicalDice' in client)
ck('scr016_requires_physics','physicalDiceAvailable()' in scr and 'Nenhum resultado mecânico foi sorteado ou animado de forma falsa.' in scr)
ck('action_consumes_physical_faces','physicalSkillRng' in action and 'usesPhysicalDice' in action)
ck('physical_does_not_advance_deterministic_rng','usesPhysicalDice ? structuredClone(state.rngState) : rng.exportState()' in action)
ck('rules_records_rng_authority','rng.algorithm ?? RNG_ALGORITHM' in rules)
ck('overlay_ptbr','DADOS 3D — WWN' in overlay and 'CANCELAR ROLAGEM' in overlay and 'Dados em movimento' in overlay)
ck('no_foreign_branding',not re.search(r'Pendragon|Strixhaven|Faer.n|Mystara|D&D', '\n'.join(p.read_text('utf-8',errors='ignore') for p in java.glob('*.java'))))
ck('no_foreign_asset_dependency','wood_clean' not in overlay and 'R.raw.dice_' not in overlay)
ck('no_fake_ui_rng','Math.random' not in client and 'Math.random' not in scr)
failed=[n for n,v in checks if not v]
for n,v in checks: print(('PASS' if v else 'FAIL'),n)
print(f'CLOSURE068_PHYSICAL_DICE_CONTRACT checks={len(checks)} pass={len(checks)-len(failed)} fail={len(failed)}')
if failed:
    print('FAILED='+','.join(failed));sys.exit(1)
