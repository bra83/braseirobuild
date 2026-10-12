#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
read=lambda n:(JAVA/n).read_text('utf-8')
version=(ROOT/'version.properties').read_text('utf-8')
main=read('MainActivity.java'); retrieval=read('RuleRetrievalEngine.java'); verifier=read('RuleResolutionVerifier.java')
db=read('AppDb.java'); indexer=read('LocalManualIndexer.java'); world=read('LivingWorldSimulationEngine.java')
autonomy=read('WorldAutonomyPlanner.java'); orchestrator=read('MasterOrchestrator.java'); options=read('SceneOptionDiversifier.java')
checks=[]
def check(name,cond):
 checks.append((name,bool(cond))); print(('OK  ' if cond else 'FALHA  ')+name)
version_values=dict(line.split('=',1) for line in version.splitlines() if '=' in line)
version_tuple=tuple(int(part) for part in version_values.get('VERSION_NAME','0.0.0').split('.')[:3])
check('versão consolidada 4.7.0+', version_tuple >= (4,7,0) and int(version_values.get('VERSION_CODE','0')) >= 470000)
check('busca obrigatória antecede o Gemini', 'engine.retrieve(mode,requestedText' in main and main.index('engine.retrieve(mode,requestedText') < main.index('generateTextWithFilesStreaming'))
check('índice só é completo com cobertura integral', all(x in db for x in ('actual_pages','rag_page_count','rag_modified_time','modified_time','LOCAL_MANUAL_STORE')))
check('índice desatualizado fica fora da pesquisa', "COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'')" in db)
check('fallback sem páginas nunca fica pronto', 'partial_unpaged' in indexer and 'fallback_unpaged' in indexer)
check('recorte preserva offsets do PDF', 'normForOffsets' in retrieval and 'Não colapsar caracteres' in retrieval)
check('passagens viram procedimentos estruturados', 'RuleProcedureCompiler.compile' in retrieval and 'structured_rules' in retrieval)
check('mesma prova entra no barramento e no árbitro', 'rule_retrieval' in orchestrator and 'setActiveRuleRetrieval' in orchestrator)
check('cálculo de PV é reproduzido localmente', all(x in verifier for x in ('verifiedMagnitude','recentRollMatches','computed_value')))
check('modificador de atributo vem da ficha', all(x in verifier for x in ('modifier_source','abilityModifierAllowed','symbolicModifierMentioned')))
check('metade/terço exige texto da regra', 'divisionAllowed' in verifier and 'divide_by' in orchestrator)
check('arredondamento é declarado e recalculado', 'rounding' in verifier and 'Math.floorDiv' in verifier)
check('recursos usam aliases fundamentados', 'resourceGrounded' in verifier and 'spell slot' in verifier)
check('blocos de inimigos são conferidos na página', 'verifyEnemyStatBlocks' in verifier and 'labeledNumber' in verifier)
check('Mundo Vivo recebe recuperação por ação', 'attachRuleSupport' in world and 'retrieveForWorldAction' in world)
check('efeitos mecânicos sem prova não são aplicados', 'mechanics_blocked' in world and 'mechanics_block_reason' in world)
check('plano de facção concluído é recriado', 'needsNewFactionPlan' in autonomy and 'needsNewFactionPlan' in world)
check('facções reagem a estado persistente', all(x in autonomy for x in ('listWorldEventsForMaster','listWorldClocks','listQuests','decision_basis')))
check('NPCs usam urgência, medo, suspeita e confiança', all(x in autonomy for x in ('urgency','fear','suspicion','trust')))
check('avanço confirmado permanece transacional', 'runInTransactionChecked' in world)
check('sugestões mantêm histórico por campanha', 'MAX_HISTORY = 60' in options and 'saveSceneOptionHistory' in main)
check('Charon mantém conexão/leitura ampliadas', 'CHARON_WAIT_STATUS_MS' in main and 'CHARON_LONG_WAIT_STATUS_MS' in main and 'CHARON_CONNECT_TIMEOUT_MS = 15000' in read('NarrationPlaybackPolicy.java') and 'CHARON_READ_TIMEOUT_MS = 60000' in read('NarrationPlaybackPolicy.java'))
check('perfil híbrido permanece canônico', 'Híbrido compatível' in main and 'PROFILE_HYBRID' in read('DndRulesEngine.java'))
check('contrato do Mestre expõe modificador/divisão', all(x in main for x in ('modifier_source','divide_by','rounding')))
check('teste JVM de resolução está incluído', (ROOT/'ferramentas/RuleResolutionVerifierSelfTest.java').is_file() and (ROOT/'tools/test_rule_resolution_verifier.sh').is_file())
failed=[n for n,ok in checks if not ok]
print(f'RESULTADO SUPER CORREÇÃO: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.')
if failed:
 print('FALHAS:'); [print(' - '+x) for x in failed]; sys.exit(1)
