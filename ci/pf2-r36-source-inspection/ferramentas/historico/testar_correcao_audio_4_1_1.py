#!/usr/bin/env python3
from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
java=root/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(java/'MainActivity.java').read_text()
smart=(java/'SmartAudioLibrary.java').read_text()
db=(java/'AudioCatalogDb.java').read_text()
gradle=(root/'android/app/build.gradle.kts').read_text()
checks=[]
def check(name, cond): checks.append((name,bool(cond)))
check('versao 4.1.1', 'versionName = "4.1.1"' in gradle and 'versionCode = 410001' in gradle)
check('marcador antes do catalogo', main.index('recoverInterruptedAudioDiagnostic();') < main.index('smartAudio=createSmartAudioSafely();'))
check('recuperacao automatica do banco auxiliar', 'AudioCatalogDb.recoverAfterInterruptedDiagnostic(this)' in main and 'AudioCatalogDb.recoverIfNeeded(context)' in smart)
check('diagnostico isolado', 'runClimateRotationDiagnostic' in smart and 'smartAudio.runClimateRotationDiagnostic(scenes)' in main)
method=main[main.index('private void showAudioRotationTest()'):main.index('private void selectAudioFolder()',main.index('private void showAudioRotationTest()'))]
check('teste nao usa fila persistente', 'taskQueue.submit' not in method)
check('teste nao grava historico', 'markPlayed' not in method)
check('teste nao toca audio', '.play' not in method and 'speak' not in method)
diag=smart[smart.index('public synchronized JSONArray runClimateRotationDiagnostic'):smart.index('public synchronized JSONObject resolveMusic',smart.index('public synchronized JSONArray runClimateRotationDiagnostic'))]
check('diagnostico nao acessa banco diretamente', 'catalogDb.' not in diag)
check('selecao nao abre metadados nativos', 'ensureDuration(chosen.scored.sound)' not in smart)
check('historico de audio nao derruba app', 'public synchronized boolean markPlayed' in db and 'catch (Throwable error)' in db)
check('banco da campanha isolado', 'audio_catalog_v2.db' in db and 'AppDb' not in db)
check('filtro Mystara estrito em D&D', 'boolean exactMystara=title.startsWith("mystara_")' in main and 'WORLD_MYSTARA.equals(world)?exactMystara:!exactMystara' in main and 'manualWorldClause' in (java/'AppDb.java').read_text())
check('sfx pontual sem loop', 'setLooping(false)' in (java/'AudioController.java').read_text() and 'SFX_DUPLICATE_WINDOW_MS' in (java/'AudioController.java').read_text())
check('ambiencia aguarda voz real', 'voiceStartedForPreparation' in (java/'NarrativeAudioEngine.java').read_text() and 'voice_confirmed_for_scene' in (java/'NarrativeAudioEngine.java').read_text())
check('fallback Charon para Android', 'fallbackToNativeNarration' in main and '7500L' in main)
failed=[n for n,ok in checks if not ok]
for n,ok in checks: print(('OK  ' if ok else 'FALHA ')+n)
print(f'RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificacoes aprovadas')
sys.exit(1 if failed else 0)
