#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
read=lambda n:(JAVA/n).read_text(encoding='utf-8')
main=read('MainActivity.java')
retrieval=read('RuleRetrievalEngine.java')
db=read('AppDb.java')
master=read('MasterOrchestrator.java')
arbiter=read('MasterRulesArbiter.java')
world=read('LivingWorldSimulationEngine.java')
autonomy=read('WorldAutonomyPlanner.java')
state=read('WorldStateEngine.java')

checks=[]
def check(name, cond):
    checks.append((name,bool(cond)))
    print(('OK  ' if cond else 'FALHA  ')+name)

# Recuperação obrigatória e não bloqueante
send=main[main.index('private void sendAi'):main.index('private void runOfflineMaster')]
check('busca obrigatória acontece para toda chamada do Mestre', 'engine.retrieve(mode,requestedText' in send)
check('busca não usa gatilho de palavras-chave', 'shouldUseLocalManualSearch(mode' not in send)
check('busca roda fora da thread de interface', 'AppExecutors.io().execute' in send)
check('Gemini só é chamado após a busca', send.index('engine.retrieve(') < send.index('continueSendAiAfterRuleSearch') < send.index('generateTextWithFilesStreaming'))
check('falha da busca fecha mecânicas', 'sufficient_for_mechanics",false' in send and 'status","error' in send)
check('modos de regras bloqueiam sem prova', 'Regras RAW/RAI' in send and 'localHits.isEmpty()' in send)
check('índice incompleto bloqueia mecânicas', 'index_incomplete' in retrieval and 'index_complete' in retrieval and 'sufficient_for_mechanics' in retrieval)
check('saúde exige todos os manuais habilitados e íntegros', 'manualIndexHealth' in retrieval and 'index_health' in retrieval)

# Consultas e ranqueamento
for token in ('hybrid-structured-v5-two-pass','searched_all_enabled_manuals','structured_rules','getManualLocalPageWindow','stateFocus'):
    check('recuperação contém '+token, token in retrieval)
check('consultas bilíngues', 'spec.pt+" "+base' in retrieval and 'spec.en+" "+base' in retrieval)
check('consulta inclui ficha, itens e magias', all(x in retrieval for x in ('equipment_json','spells_json','sheet_json')))
check('consulta inclui combate e viagem', 'listCombatants()' in retrieval and 'getTravelRulesState()' in retrieval)
check('consultas cobrem combate, magia, viagem, social, economia e mundo', all(x in retrieval for x in ('ataque_dano','magia_concentracao','viagem_terreno','social_negociacao','economia_tempo_livre','mundo_vivo')))
check('páginas vizinhas entram no contexto', 'getManualLocalPageWindow' in retrieval and 'seedCount' in retrieval)
check('limite de contexto evita estouro', 'maxChars' in retrieval and 'usedChars' in retrieval)
segment=db[db.index('searchManualLocalPages'):db.index('getManualLocalPageWindow')]
check('busca SQL ranqueia antes do limite', (('ORDER BY (' in segment and 'LIMIT 1800' in db) or ('ORDER BY LENGTH(offsets(manual_local_pages_fts)) DESC' in segment and 'safeCandidateLimit' in segment)))
check('busca SQL usa termos, radicais e frases', all(x in db[db.index('searchManualLocalPages'):db.index('getManualLocalPageWindow')] for x in ('stem','phrases','_coverage','_matched_terms')))
check('perfil híbrido considera 2014 e 2024', 'activeRules.contains("hibrido")' in db and 'rowProfile.contains("2014")' in db and 'rowProfile.contains("2024")' in db)
check('Mystara mantém autoridade própria', 'MystaraManualAuthority.scoreAdjustment' in db)

# Prova ligada ao Mestre e ao validador
check('pacote de regras entra no game_bus', 'bus.put("rule_retrieval"' in master)
check('busca da cena é mesclada com a busca do Mundo Vivo', 'worldRuleRetrieval' in master and 'mergeRuleRetrieval' in master)
check('mesclagem preserva bloqueio de índice incompleto', 'index_complete' in master[master.index('private static JSONObject mergeRuleRetrieval'):master.index('private static JSONObject compactRuleRetrieval')])
check('pacote é preservado na compactação', 'compactRuleRetrieval' in master and '"rule_retrieval"' in master[master.index('fitContext'):])
check('Mestre recebe instrução de usar só evidências recuperadas', 'use somente rule_retrieval.evidence/structured_rules' in master)
check('sem prova o Mestre deixa mecânicas vazias', 'sufficient_for_mechanics=false' in master)
check('validador recebe o mesmo pacote', 'setActiveRuleRetrieval' in master and 'activeRuleRetrieval' in arbiter)
check('citação precisa existir no índice', 'verifyEnabledManualExcerpt' in arbiter)
check('citação precisa coincidir com a busca atual', 'matchesRetrievedEvidence' in arbiter)
check('teste obrigatório recebe a evidência mais pertinente', 'bestEvidenceForTest' in arbiter and 'rule_support' in arbiter and 'pending_test' in arbiter)
check('mudança mecânica sem base é bloqueada', 'blockedMechanics' in arbiter and 'foi bloqueada' in arbiter)

# Mundo Vivo
check('simulação consulta manuais em todo avanço', 'retrieveForWorld' in world and 'rule_retrieval' in world)
check('ações de facção recebem suporte de regra', 'attachRuleSupport(retrievalEngine,factionActions' in world)
check('ações de NPC recebem suporte de regra', 'attachRuleSupport(retrievalEngine,npcActions' in world)
check('atividades recebem suporte de regra', 'attachRuleSupport(retrievalEngine,activityUpdates' in world)
check('viagens da companhia recebem suporte de regra', 'attachRuleSupport(retrievalEngine,troupeUpdates' in world)
check('efeito mecânico sem prova fica marcado', 'mechanics_blocked' in world and 'mechanics_block_reason' in world)
check('pendências carregam as páginas recuperadas', world.count('put("rule_support"') >= 3)
check('Mundo Vivo confirma tudo em transação SQLite', 'runInTransactionChecked' in world and 'Simulação autônoma confirmada' in world)
check('avanço geral inclui simulação do Mundo Vivo', 'livingSimulation.preview(report)' in state and 'livingSimulation.commit(simulation)' in state)
check('facções geram planos por guerra, comércio, religião, pesquisa e intriga', all(x in autonomy for x in ('"guerra"','"comercio"','"religiao"','"pesquisa"','"intriga"')))
check('facções criam etapas e consequências estruturais', 'plan_steps_json' in autonomy and 'knowledge_updates' in autonomy and 'world_events' in autonomy)
check('NPC decide por urgência, medo, suspeita, confiança e facção', all(x in autonomy for x in ('urgency','fear','suspicion','trust','faction')))
check('NPC e facção produzem consultas específicas de regra', autonomy.count('rule_query') >= 2)
check('ações autônomas não concedem dano, XP ou ouro diretamente', not any(x in autonomy for x in ('hp_delta','xp_delta','gold_delta','add_items')))
check('diário não é duplicado pela consequência e pelo aplicador', autonomy.count('"journal_updates"') == 2)  # apenas eventos futuros
check('eventos futuros continuam estruturados e auditáveis', 'due_day' in autonomy and 'consequence_json' in autonomy)

# Compilação e execução verificável das regras
compiler=read('RuleProcedureCompiler.java')
verifier=read('RuleResolutionVerifier.java')
indexer=read('LocalManualIndexer.java')
check('passagens viram procedimentos ordenados', all(x in compiler for x in ('procedure_steps','success_clauses','failure_clauses','action_economy','durations','ranges','targets')))
check('números só aplicam após cálculo reproduzido', all(x in verifier for x in ('recentRollMatches','verifiedMagnitude','computed_value','fixedValuePresent')))
check('multiplicadores e modificadores precisam estar na fonte', 'multiplierAllowed' in verifier and 'modifierAllowed' in verifier)
check('blocos de inimigos são conferidos na página', 'verifyEnemyStatBlocks' in verifier and 'labeledNumber' in verifier)
check('fallback sem páginas nunca fica pronto', 'partial_unpaged' in indexer and 'unpaged:' in indexer)
check('página que falha impede índice pronto', 'failedPages==0' in indexer and 'result.failedPages++' in indexer)
check('saúde do índice exige cobertura integral e versão atual', all(x in db for x in ('manualIndexHealth','actual_pages','rag_modified_time','LOCAL_MANUAL_STORE')))
check('facções reagem a eventos, relógios e missões reais', all(x in autonomy for x in ('listWorldEventsForMaster','listWorldClocks','listQuests','decision_basis','target_ref')))
check('plano autônomo reavalia ao terminar', 'put("plan_cycle",0)' in autonomy and 'plan_steps_json_after' in world)

failed=[n for n,ok in checks if not ok]
print(f'RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.')
if failed:
    print('FALHAS:')
    for n in failed: print(' - '+n)
    sys.exit(1)
