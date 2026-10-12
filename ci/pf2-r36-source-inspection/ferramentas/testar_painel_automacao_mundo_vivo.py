from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
engine = (ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e/LivingWorldEngine.java').read_text(encoding='utf-8')
main = (ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')

checks = {
    'dashboard público': 'public JSONObject automationDashboard(int recentLimit,int upcomingLimit)' in engine,
    'linha do tempo usa diário': "FROM journal WHERE world_profile=?" in engine,
    'linha do tempo usa auditoria': "FROM audit_log WHERE world_profile=?" in engine,
    'eventos futuros': "FROM world_events WHERE world_profile=? AND status IN ('scheduled','awaiting_review')" in engine,
    'atividades futuras': "FROM life_activities a" in engine and 'next_check_day' in engine,
    'relógios automáticos': "FROM world_clocks WHERE world_profile=? AND active=1" in engine,
    'planos de facções': "FROM factions WHERE world_profile=? AND active=1" in engine,
    'ações de NPC': "FROM npc_cognitive_state WHERE world_profile=?" in engine,
    'projetos de guilda': "FROM guild_projects p INNER JOIN guilds g" in engine,
    'mensalidades de guilda': "FROM guild_members gm INNER JOIN guilds g" in engine,
    'transações pendentes': "FROM pending_transactions WHERE world_profile=? AND status IN ('pending','reviewing')" in engine,
    'eventos aguardando revisão': "status='awaiting_review'" in engine,
    'atividades aguardando regra/aplicação': "a.status IN ('aguardando_regra','aguardando_aplicacao')" in engine,
    'painel inserido após calendário': 'addLivingWorldAutomationPanel(b,state,day);' in main,
    'seção últimas ações': 'ÚLTIMAS AÇÕES AUTOMÁTICAS' in main,
    'seção próximas ações': 'PRÓXIMAS AÇÕES PREVISTAS' in main,
    'seção pendências': 'FILA ÚNICA DE PENDÊNCIAS' in main and 'db.listPendingActions("pending",100)' in main,
    'diálogo de revisão': 'showLivingWorldPendingDialog' in main and 'ABRIR MESTRE IA' in main,
    'última sincronização': 'last_background_sync_at' in engine and 'última sincronização offline' in main,
    'atualização manual do painel': 'Button refresh=smallButton("ATUALIZAR"' in main,
}

failed = [name for name, ok in checks.items() if not ok]
if failed:
    for name in failed:
        print('FALHOU:', name)
    raise SystemExit(1)
print(f'OK: {len(checks)}/{len(checks)} verificações do painel de automação do Mundo Vivo.')
