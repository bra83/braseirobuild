from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
files={n:(ROOT/p).read_text('utf-8') for n,p in {
 'main':'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java',
 'markup':'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiTtsMarkupDirector.java',
 'policy':'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationPlaybackPolicy.java',
 'chunk':'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationChunker.java',
 'gem':'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java',
 'cache':'android/app/src/main/java/com/braseiro/pathfinder2e/TtsCache.java',
 'open':'android/app/src/main/java/com/braseiro/pathfinder2e/AdventureOpeningPlanner.java'}.items()}
ver=dict(x.split('=',1) for x in (ROOT/'version.properties').read_text().splitlines() if '=' in x)
checks=[]
def ck(n,v): checks.append((n,bool(v))); print(('OK  ' if v else 'FALHA ')+n)

name=ver.get('VERSION_NAME','0.0.0')
try: current=tuple(int(x) for x in name.split('.'))
except Exception: current=(0,0,0)
ck('versão R23 ou posterior',current>=(9,0,24) and int(ver.get('VERSION_CODE','0'))>=90024)
ck('runtime aplica tags', 'annotateForModel(model,sentence,delivery,intensity,pauseProfile,speaker)' in files['main'])
ck('tags 2.5 + 3.1', 'gemini-2.5' in files['markup'] and 'gemini-3.1' in files['markup'])
ck('prompt avançado de direção', all(x in files['policy'] for x in ['# AUDIO PROFILE:','## THE SCENE',"### DIRECTOR'S NOTES",'#### TRANSCRIPT']))
ck('audio_plan chega ao Charon', all(('out.put("'+x+'"') in files['main'] for x in ['delivery','intensity','pause_profile','environment','weather','location','time_of_day']))
ck('perfil NPC chega ao Charon', all(x in files['main'] for x in ['register_name','speaking_style','tempo','warmth','formality']))
ck('cache inclui direção', 'cacheDirection' in files['main'] and 'directed+"|"+normalized' in files['cache'])
ck('menos requests sem cena gigante', all(x in files['chunk'] for x in ['CHARON_SINGLE_BLOCK = 1700','CHARON_FIRST_BLOCK = 1300','CHARON_NEXT_BLOCK = 2200']))
ck('abertura estabelece conflito cedo', 'NOS PRIMEIROS 80 A 120 TERMOS' in files['main'] and 'primeira cena jogável' in files['main'].lower())
ck('abertura proíbe microtécnica como enchimento', 'microdescrição técnica de ofício' in files['main'] and 'no máximo UMA frase' in files['open'])
ck('gate detecta regressão da carpintaria', 'formao' in files['main'] and 'espiga' in files['main'] and 'ranhura' in files['main'])
ck('teste interno usa tom + tags', 'TESTAR CHARON — TOM + TAGS' in files['main'])
failed=[n for n,v in checks if not v]
print('R23: %d/%d'%(len(checks)-len(failed),len(checks)))
if failed: raise SystemExit('Falhas: '+', '.join(failed))
