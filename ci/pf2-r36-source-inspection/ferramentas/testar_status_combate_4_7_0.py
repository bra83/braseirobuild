from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
board=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/TacticalBoardView.java').read_text(encoding='utf-8')
db=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text(encoding='utf-8')
checks={
 'abas mantidas com rolagem horizontal':'HorizontalScrollView navScroll' in main and 'String[] names={"Aventura","Mestre IA"' in main,
 'badge de informação desenhado no token':'c.drawText("i",bx,by+4*density,paint)' in board,
 'toque no badge não move o token':'infoListener.onInfoRequested' in board and 'return true;' in board,
 'callback da mesa ligado ao painel':'tacticalBoardView.setInfoListener' in main,
 'ícone na lista D&D':'NPC / INIMIGO' in main and 'combatantInfoDialog(c)' in main,
 'ícone na lista Mystara':'combatantInfoDialog(rowData)' in main,
 'consulta exata por id':'public JSONObject getCombatant(long id)' in db,
 'painel de status':'private void combatantInfoDialog' in main,
 'resumo de herói e ficha completa':'ABRIR FICHA COMPLETA' in main,
 'resumo de NPC e cognição':'ESTADO DO NPC' in main and 'findNpcMemoryByName' in main,
 'status combate e defesas':'STATUS DE COMBATE' in main and 'DEFESAS ESPECIAIS' in main,
 'acessibilidade do ícone':'setContentDescription("Abrir status e resumo da ficha de "' in main,
}
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise SystemExit('FALHOU: '+', '.join(failed))
print(f'OK: {len(checks)}/{len(checks)} verificações do painel de status de combate.')
