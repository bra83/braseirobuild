from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e'
main = (SRC/'MainActivity.java').read_text(encoding='utf-8')
opts = (SRC/'SceneOptionDiversifier.java').read_text(encoding='utf-8')
opening = (SRC/'AdventureOpeningPlanner.java').read_text(encoding='utf-8')
board = (SRC/'TacticalBoardView.java').read_text(encoding='utf-8')
db = (SRC/'AppDb.java').read_text(encoding='utf-8')

checks = {
    '12 arquiteturas de abertura': opening.count('{"') >= 24 and 'private static final String[][] PLANS' in opening,
    'histórico impede molde recente': '!recent.contains(p[0])' in opening and 'Math.max(0,old.length()-7)' in opening,
    'abertura usa planejador variável': 'AdventureOpeningPlanner.choose' in main,
    'sugestões mostram no máximo seis e podem ficar vazias sem alvo concreto': 'MAX_OPTIONS = 6' in opts and 'isGroundedInScene' in opts and 'int wanted=hasScene?MAX_OPTIONS' not in opts,
    'sugestões genéricas são rejeitadas': 'agir sobre a mudanca' in opts and 'reconhecer o local' in opts,
    'fallback de sugestões usa NPC/pista/ameaça': ('PEDIR A ' in opts or 'NEGOCIAR A EXIGÊNCIA' in opts) and 'EXAMINAR ' in opts and 'MARCAR A POSIÇÃO DA AMEAÇA' in opts and 'firstEntityValue' in opts,
    'imagem preserva proporção 9:16': 'SceneImageLayoutPolicy' in main and 'ImageView.ScaleType.CENTER_CROP' in main,
    'imagem abre em tela cheia': 'showSceneImageFullscreen' in main and 'ABRIR' in main,
    'NPCs do envelope entram no elenco visual': 'buildSceneCastPreview' in main and 'present_npcs' in main,
    'NPC nominal narrado tem fallback visual': 'mergeExplicitlyNamedNpcHints' in main,
    'NPCs presentes são sincronizados ao combate': 'syncPresentNpcsIntoRunningCombat' in main,
    'controle rápido de volume no cabeçalho': 'showQuickVolumeDialog' in main and 'Abrir controles rápidos de volume' in main,
    'volumes são aplicados enquanto desliza': 'audio.setVolumes' in main,
    'Combate é a terceira aba': '"Aventura","Mestre IA","Combate","Companhia"' in main,
    'mesa tática real persiste posição': 'TacticalBoardView(this,db)' in main and 'setTacticalTokenPosition' in board,
    'tokens usam retrato e iniciativa': 'token_uri' in board and 'initiative' in board,
    'status abre pelo token': 'InfoListener' in board and 'combatantInfoDialog' in main,
    'dados rolam dentro do combate': 'addCombatDicePanel' in main and 'ROLAGEM NA BATALHA' in main,
    'narrativa aparece no rodapé do combate': 'addCombatNarrativePanel' in main and 'NARRATIVA DA BATALHA' in main,
    'ação do combate chama o Mestre no mesmo fluxo': 'RESOLVER COM O MESTRE' in main and 'COMBATE EM ANDAMENTO' in main,
    'combate abre automaticamente quando começa': 'navigate("Combate")' in main and 'combatWasRunning' in main,
    'combate registra diário': 'Combate iniciado' in main and 'addJournalChecked("Combate • Rodada' in main,
    'estado tático entra no banco': 'tactical_tokens' in db and 'listTacticalTokens' in db,
    'Mystara também recebe mesa e narrativa': main.count('addCombatNarrativePanel(b,state)') >= 2 and main.count('TacticalBoardView(this,db)') >= 2,
}

failed=[]
for name, ok in checks.items():
    print(('OK  ' if ok else 'FALHA  ')+name)
    if not ok: failed.append(name)
if failed:
    raise SystemExit(f'{len(failed)} falha(s): '+', '.join(failed))
print(f'RESULTADO EXPERIÊNCIA 4.8.0: {len(checks)}/{len(checks)} verificações aprovadas.')
