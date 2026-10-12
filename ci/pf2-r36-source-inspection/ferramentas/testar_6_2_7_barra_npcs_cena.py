from pathlib import Path
import subprocess, sys
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
EX=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NarratedNpcExtractor.java').read_text(encoding='utf-8')
SCENE=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/SceneStateEngine.java').read_text(encoding='utf-8')
AGENCY=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NpcAgencyEngine.java').read_text(encoding='utf-8')
checks=[]
def check(name, cond):
    checks.append((name,bool(cond)))
check('extrator reconhece figurantes anônimos', 'GENERIC_ACTOR' in EX and 'GROUP_PRESENT' in EX)
check('figurantes são transitórios', '.put("anonymous",true).put("transient",true)' in EX)
check('quantidade de grupos é preservada', '.put("count",count)' in EX)
check('até 12 participantes narrados entram na prévia', 'NarratedNpcExtractor.extract(narration,12)' in MAIN)
check('flags transitórias chegam ao elenco visual', 'npc.optBoolean("transient",false)' in MAIN and 'visible.put("transient",true)' in MAIN)
check('barra mostra contador de grupo', 'text("×"+count' in MAIN)
check('barra distingue GRUPO', 'return entry.optInt("count",1)>1?"GRUPO ×"' in MAIN)
check('figurante transitório não polui memória mundial', 'if(visible.optBoolean("transient",false)||visible.optBoolean("anonymous",false))continue;' in MAIN)
check('SceneState não cria memória permanente para figurante', 'if(update.optBoolean("transient",false)||update.optBoolean("anonymous",false))continue;' in SCENE)
check('Mundo Vivo não cria cognição permanente para figurante', 'if(visible.optBoolean("transient",false)||visible.optBoolean("anonymous",false))continue;' in AGENCY)
check('figurante neutro não vira combatente automaticamente', 'candidate.optBoolean("transient",false)' in MAIN and '!candidate.optBoolean("hostile",false)' in MAIN)
check('cena antiga é reparada ao reabrir', 'if(lastNarration.length()>0){mergeNarratedKnownNpcs(currentSceneCastPreview,lastNarration);mergeExplicitlyNamedNpcHints(currentSceneCastPreview,lastNarration);synchronizeVisualSceneCast();' in MAIN)
check('prompt obriga figurante sem nome na estrutura', 'Figurantes sem nome próprio recebem apenas rótulo descritivo de cena' in MAIN and 'Nunca deixe pessoas visíveis só na prosa' in MAIN)
for name,ok in checks: print(('PASS' if ok else 'FAIL')+' - '+name)
if not all(ok for _,ok in checks): sys.exit(1)
subprocess.run(['bash',str(ROOT/'tools/test_narrated_npc_extractor.sh')],check=True,cwd=ROOT)
print(f'OK - {sum(ok for _,ok in checks)}/{len(checks)} verificações estáticas + regressão JVM do extrator.')
