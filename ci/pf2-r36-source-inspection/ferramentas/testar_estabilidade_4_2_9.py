from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
j=root/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(j/'MainActivity.java').read_text(encoding='utf-8')
gem=(j/'GeminiService.java').read_text(encoding='utf-8')
db=(j/'AppDb.java').read_text(encoding='utf-8')
scene=(j/'SceneStateEngine.java').read_text(encoding='utf-8')
temporal=(j/'SceneTemporalResolver.java').read_text(encoding='utf-8')
indexer=(j/'LocalManualIndexer.java').read_text(encoding='utf-8')
checks=[]
def ck(name,cond):
    checks.append((name,bool(cond)))
    print(('OK  ' if cond else 'FALHA ')+name)
ck('versão central válida', re.search(r'^VERSION_NAME=\d+\.\d+\.\d+$',(root/'version.properties').read_text(),re.M) is not None)
ck('relógio canônico da cena', 'SceneTemporalResolver.resolve' in scene and 'clock_hour' in scene)
ck('frase luz dourada da manhã coberta', 'luz dourada da manha' in temporal and 'return "Manhã"' in temporal)
ck('faixa visual repara tempo antes do ícone', 'SceneTemporalResolver.Result temporal=SceneTemporalResolver.resolve' in main and 'scene.put("time_of_day",time)' in main)
ck('Charon 3.1 usa Interactions', '/v1beta/interactions' in gem and 'Api-Revision' in gem and '2026-05-20' in gem)
ck('Charon ainda tem fallback 2.5', 'gemini-2.5-flash-preview-tts' in gem and 'postWithRetry(candidate' in gem)
ck('fallback Android preservado', 'fallbackToNativeNarration' in main and 'speakNativeNarrationWithSfx' in main)
ck('áudio inicia fora da thread principal', 'prepareAudioStorageAndInitializeAsync' in main and 'AppExecutors.io().execute' in main)
ck('migração não apaga biblioteca de áudio', 'deleteAudioTree(new File(getFilesDir(),"audio_library"))' not in main and 'arquivos de som importados foram preservados' in main.lower())
ck('status não promete SFX com catálogo vazio', 'private String audioLayersStatus()' in main and 'biblioteca de sons está vazia' in main and 'SFX sincronizados.' not in main)
ck('aba Áudio explica como recuperar sons', 'BIBLIOTECA DE SONS VAZIA' in main and 'ATUALIZAR ÍNDICE COMPLETO' in main)
ck('sem indexação automática ao abrir ou narrar', main.count('scheduleRagIndexing(false)') <= 3 and 'handleAiResponse(raw,originalUserText);\n        scheduleRagIndexing(false);' not in main)
ck('limpeza de cache não deixa pausa presa', 'resetRagPauseState();toast("Cache limpo:' in main and 'Cache dos manuais limpo.' in main)
ck('mensagem genérica de cancelamento', 'Indexação pausada com segurança.' in indexer and 'após limpeza do cache' not in indexer)
ck('botão apagar todos os manuais', 'APAGAR TODOS OS MANUAIS' in main and 'confirmDeleteAllManuals' in main)
ck('exclusão transacional de D&D e Mystara', 'public int deleteAllManuals()' in db and 'sql.delete("manuals",null,null)' in db and 'sql.beginTransaction()' in db)
ck('exclusão desconecta pasta e suprime reentrada', 'libraryConnections.disconnect(LibraryConnectionManager.MANUALS)' in main and 'bundled_mythic_manual_suppressed' in main)
ck('nenhum teste dentro de src/main', not any((root/'android/app/src/main').rglob('*Test.java')))
failed=[n for n,c in checks if not c]
print(f'RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.')
if failed: sys.exit(1)
