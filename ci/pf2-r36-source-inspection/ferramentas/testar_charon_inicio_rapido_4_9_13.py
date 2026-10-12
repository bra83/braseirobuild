from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text()
CHUNK=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationChunker.java').read_text()
POLICY=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationPlaybackPolicy.java').read_text()
VER=dict(line.strip().split('=',1) for line in (ROOT/'version.properties').read_text().splitlines() if '=' in line)
checks=[]
def check(name, ok):
    checks.append((name,bool(ok))); print(('OK  ' if ok else 'FALHA ')+name)
check('versao 4.9.13+', int(VER.get('VERSION_CODE','0'))>=490913)
m=re.search(r'CHARON_FIRST_BLOCK\s*=\s*(\d+)',CHUNK); first=int(m.group(1)) if m else 9999
single=re.search(r'CHARON_SINGLE_BLOCK\s*=\s*(\d+)',CHUNK); single=int(single.group(1)) if single else 9999
check('primeiro bloco <= 360 caracteres', first<=360)
check('cena media nao e enviada inteira ate 1100 caracteres', single<=400)
check('pipeline cache mudou de versao', 'charon-v11-primeira-fala-rapida' in POLICY)
check('sem varredura previa de todos os blocos', 'countUncachedTts' not in MAIN)
check('playback e enfileirado antes do cache', MAIN.find('audio.enqueueTimedVoice(playable,true') < MAIN.find('ttsCache.store(cacheModel,voice,sentence,playable)'))
check('cache ocorre em executor de IO', 'AppExecutors.io().execute(new Runnable(){public void run(){' in MAIN and 'ttsCache.store(cacheModel,voice,sentence,playable)' in MAIN)
check('proximo bloco sintetiza em paralelo com reproducao', 'synthesizeChunk(chunks,index+1,session,coordinated);' in MAIN)
check('fallback Android preservado', 'fallbackToNativeNarration' in MAIN and 'ensureNativeTtsInitialized' in MAIN)
failed=[n for n,o in checks if not o]
print(f'RESULTADO 4.9.13: {len(checks)-len(failed)}/{len(checks)}')
if failed: raise SystemExit('Falhas: '+', '.join(failed))
