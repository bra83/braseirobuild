#!/usr/bin/env python3
from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
MAP=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MapBoardView.java').read_text()
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text()
APP=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text()
VER=(ROOT/'version.properties').read_text()
checks=[]
def check(name, ok):
    checks.append((name,bool(ok))); print(('OK  ' if ok else 'FALHA  ')+name)
version=dict(line.strip().split('=',1) for line in VER.splitlines() if '=' in line)
check('versão 4.9.11+', int(version.get('VERSION_CODE','0'))>=490911)
check('mapa assume gesto ao tocar', 'requestDisallowInterceptTouchEvent(true)' in MAP)
check('mapa libera gesto ao terminar', 'requestDisallowInterceptTouchEvent(false)' in MAP)
check('pan horizontal continua somando delta X', 'offsetX += event.getX() - lastX' in MAP)
check('pan vertical continua somando delta Y', 'offsetY += event.getY() - lastY' in MAP)
check('zoom continua ativo', 'scaleDetector.onTouchEvent(event)' in MAP and 'detector.getScaleFactor()' in MAP)
check('página permanece rolável fora do mapa', 'final ScrollView mapPageScroll=new ScrollView(this)' in MAIN and 'Fora do mapa: deslize a página' in MAIN)
check('escolha de abertura destrava mapa temporariamente', 'if(pendingAdventureStartFromMap)mapLockedForScreen=false' in MAIN)
check('hex não catalogado procura vizinho catalogado', 'nearestCatalogedAdventureStart(hexId)' in MAIN)
check('consulta de proximidade existe', 'findNearestAdventureStartCandidates' in APP)
check('proximidade usa distância 2D', 'AS distance2' in APP and 'ORDER BY distance2' in APP)
check('proximidade mantém mesmos tipos narrativos', "'cidade','vila','porto','fortaleza','castelo','ruina','masmorra','templo','caverna'" in APP)
check('proximidade mantém filtro de terreno confiável', 'terrain_confidence,0)>=0.60' in APP and "'water','sea','ocean','coast_water','map_border'" in APP)
check('toque inválido não termina em toast de rejeição', 'toast("Esse hex não contém cidade, local nomeado ou terreno catalogado para uma abertura.")' not in MAIN)
check('usuário confirma encaixe no ponto próximo', 'O ponto catalogado mais próximo é ' in MAIN and 'CONFIRMAR E INICIAR' in MAIN)
check('sorteio aleatório preservado', 'AdventureStartSelector.choose' in MAIN and 'SORTEAR COM VARIEDADE' in MAIN)
check('busca rápida de regras preservada', 'searchManualLocalPagesFast' in (ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/RuleRetrievalEngine.java').read_text())
failed=[name for name,ok in checks if not ok]
print(f'RESULTADO 4.9.11: {len(checks)-len(failed)}/{len(checks)}')
if failed:
    print('FALHAS:'); [print(' - '+x) for x in failed]; sys.exit(1)
