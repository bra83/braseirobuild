from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
manifest=(ROOT/'android/app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
main=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
version=(ROOT/'version.properties').read_text(encoding='utf-8')
checks=[]
def check(name, cond):
    checks.append((name,bool(cond)))
check('versao 6.2.6', 'VERSION_NAME=6.2.6' in version and 'VERSION_CODE=602600' in version)
check('schema preservado', 'DB_SCHEMA=57' in version and 'BACKUP_FORMAT=20' in version)
check('retrato por sensor', 'android:screenOrientation="sensorPortrait"' in manifest)
check('orientation configChanges', 'orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden' in manifest)
check('handler onConfigurationChanged', 'public void onConfigurationChanged(Configuration newConfig)' in main)
config_body=main.split('public void onConfigurationChanged(Configuration newConfig)',1)[1].split('private boolean isKnownScreen',1)[0]
check('sem beginAdventure no config handler', 'beginAdventure(' not in config_body)
check('salva tela atual', 'outState.putString(STATE_CURRENT_SCREEN' in main)
check('restaura tela atual', 'state.getString(STATE_CURRENT_SCREEN' in main and 'navigate(restoredScreen)' in main)
check('salva rascunho', 'outState.putString(STATE_NARRATOR_DRAFT' in main)
check('protege manutenção na configuração', 'maintenance!=null&&!isChangingConfigurations()' in main)
check('não força showCampaign em configuração', 'showCampaign(' not in config_body)
check('não reinicializa DB em configuração', 'new AppDb' not in config_body)
for name,ok in checks: print(('PASS' if ok else 'FAIL')+': '+name)
failed=[n for n,o in checks if not o]
print(f'\nResultado: {len(checks)-len(failed)}/{len(checks)} PASS')
sys.exit(1 if failed else 0)
